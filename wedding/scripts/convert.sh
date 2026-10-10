#!/bin/sh
# 원본 사진 → data/photos/NN.webp (1600px, q82). 사용: scripts/convert.sh ~/Pictures/wedding-originals
# 필요: brew install webp   (cwebp)
set -e
src="$1"; out="$(dirname "$0")/../data/photos"; mkdir -p "$out"
i=1
for f in "$src"/*.[jJ][pP][gG] "$src"/*.[jJ][pP][eE][gG] "$src"/*.[pP][nN][gG]; do
  [ -f "$f" ] || continue
  cwebp -q 82 -resize 1600 0 -quiet "$f" -o "$out/$(printf '%02d' $i).webp"
  i=$((i+1))
done
echo "$((i-1)) photos → $out"
