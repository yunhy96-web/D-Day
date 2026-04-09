#!/usr/bin/env python3
import undetected_chromedriver as uc
from selenium.webdriver.common.by import By
import time

options = uc.ChromeOptions()
options.add_argument("--window-position=-9999,-9999")
options.add_argument("--no-sandbox")
options.add_argument("--window-size=1920,1080")
driver = uc.Chrome(options=options, version_main=145)
driver.get("https://www.ralphlauren.co.kr/vintage-5-pocket-east-west-selvedge-jean-600419.html?cgid=men-rrl")
time.sleep(8)

# li.variations-attribute 의 내부 HTML 확인
els = driver.find_elements(By.CSS_SELECTOR, "li.variations-attribute")
print(f"총 {len(els)}개 사이즈 요소")
for i, e in enumerate(els):
    inner = e.get_attribute("innerHTML").strip()
    outer = e.get_attribute("outerHTML").strip()
    # outerHTML에서 처음 200자만
    print(f"\n[{i}] class='{e.get_attribute('class')}'")
    print(f"    innerHTML (first 200): {inner[:200]}")

driver.quit()
