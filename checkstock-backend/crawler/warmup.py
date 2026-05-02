#!/usr/bin/env python3
"""
CheckStock Crawler 프로필 워밍업 스크립트

목적: PerimeterX 캡차를 사람이 직접 풀어 _px3 쿠키를 프로필에 심어두기 위함.
      이후 crawl.py 가 같은 프로필을 재사용하면 캡차 없이 통과.

사용법:
    python3 crawler/warmup.py <URL>
    (URL 생략 시 기본 랄프로렌 상품 페이지)

실행 후 Chrome 창에서 캡차가 보이면 직접 "길게 누르기" 버튼을 누르세요.
페이지 로딩이 끝나고 상품 페이지가 정상적으로 보이면 엔터를 눌러 종료.
"""
import os
import sys
from pathlib import Path
import undetected_chromedriver as uc


PROFILE_DIR = str(Path(__file__).resolve().parent / ".chrome_profile")
DEFAULT_URL = "https://www.ralphlauren.co.kr/men/brands/double-rl?prefn1=CategoryCode&prefv1=%EB%8D%B0%EB%8B%98"

CHROME_BINARIES = [
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    "/Applications/Google Chrome Canary.app/Contents/MacOS/Google Chrome Canary",
    "google-chrome",
    "chromium",
]


def detect_chrome_major():
    import re
    import subprocess
    for binary in CHROME_BINARIES:
        try:
            out = subprocess.check_output([binary, "--version"], text=True, stderr=subprocess.DEVNULL).strip()
            m = re.search(r"(\d+)\.\d+\.\d+\.\d+", out)
            if m:
                return int(m.group(1))
        except Exception:
            continue
    return None


def main():
    url = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_URL
    os.makedirs(PROFILE_DIR, exist_ok=True)

    options = uc.ChromeOptions()
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--window-size=1920,1080")
    options.add_argument("--no-first-run")
    options.add_argument("--no-default-browser-check")
    options.add_argument(f"--user-data-dir={PROFILE_DIR}")

    chrome_major = detect_chrome_major()
    if chrome_major:
        driver = uc.Chrome(options=options, version_main=chrome_major)
    else:
        driver = uc.Chrome(options=options)
    try:
        driver.get(url)
        print(f"[warmup] 페이지 열림: {url}")
        print("[warmup] 캡차가 뜨면 직접 '길게 누르기' 버튼을 누르세요.")
        print("[warmup] 상품 페이지가 정상적으로 보이면 이 터미널에서 엔터를 눌러 종료합니다.")
        input()
        print(f"[warmup] 프로필 저장 위치: {PROFILE_DIR}")
    finally:
        driver.quit()


if __name__ == "__main__":
    main()
