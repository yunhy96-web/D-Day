#!/usr/bin/env python3
"""
CheckStock Crawler - undetected-chromedriver 기반
Spring Boot에서 호출하여 사이즈 정보를 JSON으로 반환
"""
import sys
import json
import undetected_chromedriver as uc
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time


def crawl(url, size_selector="li.variations-attribute", sold_out_indicator="out", target_size=None):
    options = uc.ChromeOptions()
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--window-size=1920,1080")
    options.add_argument("--window-position=-9999,-9999")  # 화면 밖으로
    options.add_argument("--no-first-run")
    options.add_argument("--no-default-browser-check")

    driver = uc.Chrome(options=options, version_main=145)
    try:
        driver.get(url)

        # 페이지 로드 대기
        WebDriverWait(driver, 15).until(
            lambda d: d.execute_script("return document.readyState") == "complete"
        )
        time.sleep(3)

        # targetSize가 있으면 해당 허리 사이즈 클릭 후 페이지 리프레시 대기
        if target_size:
            try:
                # aria-label이 "사이즈 선택: 32" 또는 "Selected 사이즈 선택: 32" 일 수 있음
                # XPath contains로 매칭
                size_xpath = f'//a[contains(@aria-label, "사이즈 선택: {target_size}")]'
                size_link = WebDriverWait(driver, 10).until(
                    EC.presence_of_element_located((By.XPATH, size_xpath))
                )
                # JavaScript 클릭 (요소가 가려져도 동작)
                driver.execute_script("arguments[0].click();", size_link)
                # 클릭 후 페이지 리프레시 대기
                time.sleep(5)
                WebDriverWait(driver, 15).until(
                    lambda d: d.execute_script("return document.readyState") == "complete"
                )
                time.sleep(2)
            except Exception as click_err:
                print(json.dumps({
                    "success": False,
                    "error": f"사이즈 '{target_size}' 클릭 실패: {str(click_err)}",
                    "sizes": [],
                    "availableCount": 0,
                    "totalCount": 0,
                    "title": driver.title
                }, ensure_ascii=False))
                return

        # 사이즈 요소 추출
        elements = driver.find_elements(By.CSS_SELECTOR, size_selector)
        raw_sizes = []

        for el in elements:
            class_attr = el.get_attribute("class") or ""
            available = sold_out_indicator not in class_attr

            # 1차: innerText로 라벨 추출
            label = el.text.strip()

            # 2차: innerText가 비면 aria-label에서 추출 (Ralph Lauren 패턴)
            if not label:
                anchor = el.find_elements(By.CSS_SELECTOR, "a[aria-label]")
                if anchor:
                    aria = anchor[0].get_attribute("aria-label") or ""
                    # "사이즈 선택: 30" → "30", "길이 선택: 32" → "32"
                    if ":" in aria:
                        prefix = aria.split(":")[0].strip()
                        label = aria.split(":")[-1].strip()
                        if "길이" in prefix:
                            raw_sizes.append({"label": label, "available": available, "group": "기장"})
                            continue
                        elif "사이즈" in prefix:
                            raw_sizes.append({"label": label, "available": available, "group": "허리"})
                            continue
                    # aria-label에 사이즈/길이가 없으면 색상 스와치 → 스킵
                    continue

            # innerText가 있지만 숫자가 아니면 색상일 가능성 → 스킵
            if label and not any(c.isdigit() for c in label):
                continue

            raw_sizes.append({
                "label": label,
                "available": available,
                "group": None
            })

        # 그룹 자동 분류
        sizes = assign_groups(raw_sizes)

        result = {
            "success": True,
            "sizes": sizes,
            "availableCount": sum(1 for s in sizes if s["available"]),
            "totalCount": len(sizes),
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
    # 이미 aria-label에서 그룹이 할당된 경우 그대로 반환
    if any(s["group"] is not None for s in raw_sizes):
        return raw_sizes

    # 그룹 없으면 중복 라벨 기준으로 자동 분류
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
