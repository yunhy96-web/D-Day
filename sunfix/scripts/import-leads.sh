#!/bin/bash
# 영업 리드 가져오기(설계 §8-1). 실제 업체 연락처라 JSON 은 docs/private/(커밋 안 됨)에 두고 이 스크립트로 올린다.
# 사용: ADMIN_PW='...' scripts/import-leads.sh docs/private/leads-2026-10.json [https://sunfix.union-selim.com]
# 같은 (카테고리, 상호)는 조사 칸만 갱신하고 상태·메모·업체 연결은 그대로 둔다.
set -eu
FILE=$1; B=${2:-http://localhost:8092}
J=$(mktemp); trap 'rm -f $J' EXIT
code=$(curl -s -o /dev/null -w '%{http_code}' -c $J -H 'Content-Type: application/json' -H "Origin: $B" \
  -X POST "$B/admin/api/login" -d "$(python3 -c 'import json,os;print(json.dumps({"loginId":"sunflower","password":os.environ["ADMIN_PW"]}))')")
[ "$code" = 204 ] || { echo "로그인 실패($code)"; exit 1; }
curl -sf -b $J -H 'Content-Type: application/json' -H "Origin: $B" -X POST "$B/admin/api/leads/import" --data-binary @"$FILE"; echo
