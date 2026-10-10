#!/bin/sh
# 원본 → data/photos/NN.(webp|jpg), 긴 변 1600px. 원본은 data/originals/ 에 두고 실행.
# cwebp 있으면 webp(q82), 없으면 macOS sips 로 jpg(q82). 00-main.* 이 있으면 그게 01 = 인트로/OG 대표사진.
set -e
src="${1:-$(dirname "$0")/../data/originals}"; out="$(dirname "$0")/../data/photos"
mkdir -p "$out"; rm -f "$out"/*.svg "$out"/[0-9][0-9].webp "$out"/[0-9][0-9].jpg
i=1
for f in $(ls "$src" | grep -iE '\.(jpe?g|png|heic)$' | sort); do
  n=$(printf '%02d' $i)
  if command -v cwebp >/dev/null; then cwebp -q 82 -resize 1600 0 -quiet "$src/$f" -o "$out/$n.webp"
  else sips -Z 1600 -s format jpeg -s formatOptions 82 "$src/$f" --out "$out/$n.jpg" >/dev/null; fi
  i=$((i+1))
done
first=$(ls "$out" | grep -E '^01\.' | head -1)
[ -n "$first" ] && sips -Z 1200 -s format jpeg -s formatOptions 80 "$out/$first" --out "$out/og.jpg" >/dev/null
echo "$((i-1)) photos → $out (대표: $first, og.jpg 생성)"
