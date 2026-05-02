#!/usr/bin/env python3
"""
CheckStock Crawler - undetected-chromedriver 기반
Spring Boot에서 호출하여 사이즈 정보를 JSON으로 반환
targetSize에 콤마로 여러 허리 사이즈 지정 가능 (예: "30,31,32")
"""
import os
import sys
import json
from pathlib import Path
import undetected_chromedriver as uc
from selenium.webdriver.common.by import By
from selenium.webdriver.common.action_chains import ActionChains
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time


# Chrome 프로필을 영속화해 PerimeterX 쿠키(_px3 등)를 재사용
PROFILE_DIR = str(Path(__file__).resolve().parent / ".chrome_profile")


CAPTCHA_SELECTORS = [
    (By.XPATH, '//*[contains(text(),"길게 누르기")]'),
    (By.XPATH, '//*[contains(text(),"Press & Hold")]'),
    (By.XPATH, '//*[contains(text(),"계속하기 전에")]'),
    (By.ID, "px-captcha"),
]


def _find_captcha_button(driver):
    """현재 프레임에서 캡차 버튼을 찾음. 못 찾으면 iframe들도 확인."""
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
                            return el  # iframe context 유지
                    except Exception:
                        continue
            driver.switch_to.default_content()
        except Exception:
            driver.switch_to.default_content()
            continue

    return None


def solve_press_and_hold(driver, max_attempts=3):
    """PerimeterX press-and-hold 캡차 감지 및 자동 해결 시도.
    성공/캡차 없음이면 True, 모두 실패하면 False."""
    for attempt in range(max_attempts):
        button = _find_captcha_button(driver)
        if not button:
            return True

        hold_duration = 10 + attempt * 2  # 10, 12, 14초
        print(f"[captcha] detected, attempt {attempt + 1}/{max_attempts}, hold {hold_duration}s", file=sys.stderr)

        try:
            ActionChains(driver).move_to_element(button).perform()
            time.sleep(0.3)
            ActionChains(driver).click_and_hold(button).perform()
            time.sleep(hold_duration)
            ActionChains(driver).release().perform()
        except Exception as e:
            print(f"[captcha] action failed: {e}", file=sys.stderr)

        driver.switch_to.default_content()
        time.sleep(3)

    return False


def click_waist_size(driver, waist):
    """허리 사이즈를 클릭하고 페이지 리프레시 대기"""
    size_xpath = f'//a[contains(@aria-label, "사이즈 선택: {waist}")]'
    size_link = WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, size_xpath))
    )
    driver.execute_script("arguments[0].click();", size_link)
    time.sleep(5)
    WebDriverWait(driver, 15).until(
        lambda d: d.execute_script("return document.readyState") == "complete"
    )
    time.sleep(2)


def extract_sizes(driver, size_selector, sold_out_indicator):
    """현재 페이지에서 사이즈 정보를 추출"""
    elements = driver.find_elements(By.CSS_SELECTOR, size_selector)
    raw_sizes = []

    for el in elements:
        class_attr = el.get_attribute("class") or ""
        available = sold_out_indicator not in class_attr

        label = el.text.strip()

        if not label:
            anchor = el.find_elements(By.CSS_SELECTOR, "a[aria-label]")
            if anchor:
                aria = anchor[0].get_attribute("aria-label") or ""
                if ":" in aria:
                    prefix = aria.split(":")[0].strip()
                    label = aria.split(":")[-1].strip()
                    if "길이" in prefix:
                        raw_sizes.append({"label": label, "available": available, "group": "기장"})
                        continue
                    elif "사이즈" in prefix:
                        raw_sizes.append({"label": label, "available": available, "group": "허리"})
                        continue
                continue

        if label and not any(c.isdigit() for c in label):
            continue

        raw_sizes.append({
            "label": label,
            "available": available,
            "group": None
        })

    return assign_groups(raw_sizes)


def crawl(url, size_selector="li.variations-attribute", sold_out_indicator="out", target_size=None):
    os.makedirs(PROFILE_DIR, exist_ok=True)

    options = uc.ChromeOptions()
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--window-size=1920,1080")
    # 캡차 press-and-hold가 포커스를 요구하므로 창을 화면 밖으로 숨기지 않음
    options.add_argument("--no-first-run")
    options.add_argument("--no-default-browser-check")
    options.add_argument(f"--user-data-dir={PROFILE_DIR}")

    driver = uc.Chrome(options=options)
    try:
        driver.get(url)

        WebDriverWait(driver, 15).until(
            lambda d: d.execute_script("return document.readyState") == "complete"
        )
        time.sleep(3)

        if not solve_press_and_hold(driver):
            raise RuntimeError("press-and-hold captcha 해결 실패")

        all_sizes = []

        if target_size:
            # 콤마로 여러 허리 사이즈 지원 (예: "30,31,32")
            waist_list = [s.strip() for s in target_size.split(",")]

            for waist in waist_list:
                try:
                    click_waist_size(driver, waist)
                    sizes = extract_sizes(driver, size_selector, sold_out_indicator)

                    # 각 사이즈에 허리 정보 추가 (기장 사이즈에 어떤 허리인지 표시)
                    for s in sizes:
                        if s["group"] == "기장":
                            s["label"] = f'{s["label"]}'
                            s["group"] = f"허리{waist} 기장"
                        elif s["group"] == "허리":
                            # 허리 사이즈 자체는 이미 알고 있으므로 스킵
                            continue
                        else:
                            s["group"] = f"허리{waist}"
                    # 허리 사이즈 항목 제외, 기장만 추가
                    all_sizes.extend([s for s in sizes if "기장" in (s["group"] or "")])
                except Exception as click_err:
                    # 해당 허리 사이즈 클릭 실패 시 스킵하고 다음으로
                    all_sizes.append({
                        "label": waist,
                        "available": False,
                        "group": f"허리{waist} (클릭실패)"
                    })
        else:
            all_sizes = extract_sizes(driver, size_selector, sold_out_indicator)

        result = {
            "success": True,
            "sizes": all_sizes,
            "availableCount": sum(1 for s in all_sizes if s["available"]),
            "totalCount": len(all_sizes),
            "title": driver.title
        }
        print(json.dumps(result, ensure_ascii=False))

    except Exception as e:
        print(json.dumps({
            "success": False,
            "error": str(e),
            "title": driver.title if driver else "",
            "sizes": [],
            "availableCount": 0,
            "totalCount": 0
        }, ensure_ascii=False))
    finally:
        driver.quit()


def assign_groups(raw_sizes):
    if any(s["group"] is not None for s in raw_sizes):
        return raw_sizes

    seen = set()
    split_index = -1

    for i, s in enumerate(raw_sizes):
        if s["label"] in seen:
            split_index = i
            break
        seen.add(s["label"])

    if split_index == -1:
        return raw_sizes

    result = []
    for i, s in enumerate(raw_sizes):
        group = "허리" if i < split_index else "기장"
        result.append({
            "label": s["label"],
            "available": s["available"],
            "group": group
        })
    return result


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({"success": False, "error": "URL required"}))
        sys.exit(1)

    url = sys.argv[1]
    selector = sys.argv[2] if len(sys.argv) > 2 else "li.variations-attribute"
    indicator = sys.argv[3] if len(sys.argv) > 3 else "out"
    target_size = sys.argv[4] if len(sys.argv) > 4 else None

    crawl(url, selector, indicator, target_size)
