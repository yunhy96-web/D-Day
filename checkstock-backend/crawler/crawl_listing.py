#!/usr/bin/env python3
"""
리스팅 페이지 크롤러 (undetected_chromedriver 기반)

목적: PerimeterX로 보호된 카테고리 페이지에서 상품 목록(이름 + URL) 추출
프로필: ./.chrome_profile (warmup.py와 공유)

사용법:
    python3 crawl_listing.py <URL> <CSS_SELECTOR> <BASE_URL>

표준출력 (성공):
    {"success": true, "products": [{"name":"...", "url":"..."}, ...]}

표준출력 (실패):
    {"success": false, "error": "..."}
"""
import os
import re
import sys
import json
import time
import random
import subprocess
from pathlib import Path

import undetected_chromedriver as uc
from selenium.webdriver.common.by import By
from selenium.webdriver.common.action_chains import ActionChains
from selenium.webdriver.support.ui import WebDriverWait
from selenium.common.exceptions import NoSuchWindowException, WebDriverException


PROFILE_DIR = str(Path(__file__).resolve().parent / ".chrome_profile")

CHROME_BINARIES = [
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    "/Applications/Google Chrome Canary.app/Contents/MacOS/Google Chrome Canary",
    "google-chrome",
    "chromium",
]


def detect_chrome_major():
    for binary in CHROME_BINARIES:
        try:
            out = subprocess.check_output([binary, "--version"], text=True, stderr=subprocess.DEVNULL).strip()
            m = re.search(r"(\d+)\.\d+\.\d+\.\d+", out)
            if m:
                return int(m.group(1))
        except Exception:
            continue
    return None

CAPTCHA_SELECTORS = [
    (By.XPATH, '//*[contains(text(),"길게 누르기")]'),
    (By.XPATH, '//*[contains(text(),"Press & Hold")]'),
    (By.XPATH, '//*[contains(text(),"계속하기 전에")]'),
    (By.ID, "px-captcha"),
]


def log(msg: str) -> None:
    print(f"[crawl_listing] {msg}", file=sys.stderr, flush=True)


def find_captcha_button(driver):
    driver.switch_to.default_content()
    for by, sel in CAPTCHA_SELECTORS:
        for el in driver.find_elements(by, sel):
            try:
                if el.is_displayed():
                    return el
            except Exception:
                continue

    for iframe in driver.find_elements(By.TAG_NAME, "iframe"):
        try:
            driver.switch_to.frame(iframe)
            for by, sel in CAPTCHA_SELECTORS:
                for el in driver.find_elements(by, sel):
                    try:
                        if el.is_displayed():
                            return el
                    except Exception:
                        continue
            driver.switch_to.default_content()
        except Exception:
            driver.switch_to.default_content()
            continue
    return None


def solve_press_and_hold(driver, max_attempts: int = 2) -> bool:
    """사람처럼 자연스럽게 press-and-hold 캡차 풀기.
    - PerimeterX 최신 버전은 자동화 패턴을 잘 잡아내므로 시도 횟수를 적게 가져감
    - 실패 시 warmup.py로 수동 풀이 권장
    """
    for attempt in range(max_attempts):
        try:
            button = find_captcha_button(driver)
        except (NoSuchWindowException, WebDriverException) as e:
            log(f"window already closed: {type(e).__name__}")
            return False
        if not button:
            return True

        hold_duration = random.uniform(11.0, 13.0) + attempt * 1.5
        log(f"captcha detected, attempt {attempt + 1}/{max_attempts}, hold {hold_duration:.1f}s")

        try:
            # 1단계: 버튼 근처로 비껴서 이동 (사람은 한 번에 정확히 안 감)
            try:
                ActionChains(driver).move_to_element_with_offset(
                    button, random.randint(-15, 15), random.randint(-8, 8)
                ).perform()
            except Exception:
                pass
            time.sleep(random.uniform(0.15, 0.35))

            # 2단계: 정확한 버튼 위치로 이동
            ActionChains(driver).move_to_element(button).perform()
            time.sleep(random.uniform(0.2, 0.5))

            # 3단계: 누르기 시작
            ActionChains(driver).click_and_hold(button).perform()

            # 4단계: hold 동안 미세 떨림
            elapsed = 0.0
            while elapsed < hold_duration:
                step = random.uniform(0.4, 1.2)
                time.sleep(step)
                elapsed += step
                try:
                    ActionChains(driver).move_by_offset(
                        random.randint(-1, 1), random.randint(-1, 1)
                    ).perform()
                except Exception:
                    pass

            # 5단계: 떼기
            ActionChains(driver).release().perform()
        except (NoSuchWindowException, WebDriverException) as e:
            # 창이 닫히면 더 시도해봐야 무의미
            log(f"window closed during action: {type(e).__name__}")
            return False
        except Exception as e:
            log(f"action failed: {e}")
            try:
                ActionChains(driver).release().perform()
            except Exception:
                pass

        try:
            driver.switch_to.default_content()
        except (NoSuchWindowException, WebDriverException):
            return False
        time.sleep(random.uniform(2.5, 4.0))

    return False


def crawl(url: str, selector: str, base_url: str) -> dict:
    os.makedirs(PROFILE_DIR, exist_ok=True)

    options = uc.ChromeOptions()
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--window-size=1920,1080")
    options.add_argument("--no-first-run")
    options.add_argument("--no-default-browser-check")
    options.add_argument(f"--user-data-dir={PROFILE_DIR}")

    driver = None
    try:
        chrome_major = detect_chrome_major()
        if chrome_major:
            log(f"chrome detected: v{chrome_major}")
            driver = uc.Chrome(options=options, version_main=chrome_major)
        else:
            log("chrome version not detected, using default")
            driver = uc.Chrome(options=options)

        # Chrome 초기화 직후 driver protocol 안정화 대기 (uc 3.5.x에서 driver.get 너무 빨리 호출 시 NoSuchWindow 발생)
        time.sleep(2.5)

        # 빈 about:blank 탭에서 시작해서 연결 안정화
        try:
            driver.get("about:blank")
            time.sleep(0.5)
        except Exception as e:
            log(f"about:blank failed (무시 가능): {e}")

        log(f"navigating: {url}")
        # NoSuchWindow 발생 시 한 번 재시도
        last_err = None
        for attempt in range(2):
            try:
                driver.get(url)
                last_err = None
                break
            except Exception as nav_err:
                last_err = nav_err
                log(f"navigation attempt {attempt + 1} failed: {type(nav_err).__name__}: {nav_err}")
                time.sleep(2)
        if last_err is not None:
            raise last_err

        WebDriverWait(driver, 15).until(
            lambda d: d.execute_script("return document.readyState") == "complete"
        )
        time.sleep(3)

        if not solve_press_and_hold(driver):
            return {
                "success": False,
                "error": (
                    "PerimeterX 캡차 자동 해결 실패. "
                    "warmup.py를 수동으로 실행해서 캡차를 한 번 풀어주세요. "
                    "(터미널: cd checkstock-backend && python3 crawler/warmup.py)"
                ),
            }

        time.sleep(2)

        elements = driver.find_elements(By.CSS_SELECTOR, selector)
        log(f"selector matched: {len(elements)} elements")

        seen = set()
        products = []
        for el in elements:
            try:
                name = (el.get_attribute("textContent") or "").strip()
                if not name:
                    name = (el.get_attribute("title") or "").strip()
                if not name:
                    name = (el.get_attribute("aria-label") or "").strip()
                href = el.get_attribute("href") or ""
            except Exception:
                continue
            if not name:
                continue
            if href and not href.startswith("http"):
                href = base_url.rstrip("/") + "/" + href.lstrip("/")
            if name in seen:
                continue
            seen.add(name)
            products.append({"name": name, "url": href})

        if not products and elements:
            try:
                first_html = elements[0].get_attribute("outerHTML") or ""
                log(f"first element outerHTML (first 300 chars): {first_html[:300]}")
            except Exception:
                pass

        return {"success": True, "products": products}
    except Exception as e:
        return {"success": False, "error": f"{type(e).__name__}: {e}"}
    finally:
        if driver is not None:
            try:
                driver.quit()
            except Exception:
                pass


def main():
    if len(sys.argv) < 4:
        print(json.dumps({"success": False, "error": "args required: URL, SELECTOR, BASE_URL"}))
        sys.exit(1)

    url = sys.argv[1]
    selector = sys.argv[2]
    base_url = sys.argv[3]

    result = crawl(url, selector, base_url)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
