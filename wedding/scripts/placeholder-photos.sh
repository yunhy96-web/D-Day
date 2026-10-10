#!/bin/sh
# 개발용 더미 사진 40장 (SVG). 실제 사진은 convert.sh 로 webp 변환 후 data/photos 에.
set -e
d="$(dirname "$0")/../data/photos"; mkdir -p "$d"
i=1; while [ $i -le 40 ]; do
  n=$(printf '%02d' $i); h=$(( (i * 37) % 360 ))
  printf '<svg xmlns="http://www.w3.org/2000/svg" width="600" height="800"><rect width="100%%" height="100%%" fill="hsl(%d 35%% 70%%)"/><text x="50%%" y="50%%" font-size="120" text-anchor="middle" dominant-baseline="middle" fill="#fff" font-family="serif">%s</text></svg>' "$h" "$n" > "$d/$n.svg"
  i=$((i+1))
done
echo "40 placeholder photos → $d"
