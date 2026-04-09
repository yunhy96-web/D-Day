#!/usr/bin/env python3
import undetected_chromedriver as uc
from selenium.webdriver.common.by import By
import time, json

options = uc.ChromeOptions()
options.add_argument("--window-position=-9999,-9999")
options.add_argument("--no-sandbox")
options.add_argument("--window-size=1920,1080")
driver = uc.Chrome(options=options, version_main=145)
driver.get("https://www.ralphlauren.co.kr/vintage-5-pocket-east-west-selvedge-jean-600419.html?cgid=men-rrl")
time.sleep(5)
print("Title:", driver.title)

# variations-attribute
els = driver.find_elements(By.CSS_SELECTOR, "li.variations-attribute")
print(f"li.variations-attribute: {len(els)}개")
for e in els[:5]:
    txt = e.text.strip()
    cls = e.get_attribute("class")
    print(f"  text='{txt}' class='{cls}'")

# 다른 셀렉터 탐색
selectors = [
    "li[class*='size']",
    ".size-list li",
    "select.variation-select option",
    ".size-attribute li",
    "[class*='variation'] li",
    ".product-variations li",
]
for sel in selectors:
    found = driver.find_elements(By.CSS_SELECTOR, sel)
    if found:
        print(f"\n{sel}: {len(found)}개")
        for f in found[:5]:
            txt = f.text.strip()
            cls = f.get_attribute("class")
            print(f"  text='{txt}' class='{cls}'")

# 페이지 소스에서 사이즈 관련 힌트 찾기
src = driver.page_source
for keyword in ["variations-attribute", "size-attribute", "selectable", "out-of-stock"]:
    count = src.count(keyword)
    if count > 0:
        print(f"\nKeyword '{keyword}' found {count} times in page source")

driver.quit()
