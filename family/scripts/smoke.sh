#!/bin/bash
# 기동 중인 앱 전체 흐름 점검: 로그인 → 가입 신청 → 승인 → 글·댓글·하트 → 근무표 → 권한·격리.
# 사용: ADMIN_PW='...' scripts/smoke.sh [http://localhost:8091]   (빈 DB 에서 한 번. 테스트 회원 smoke<시각> 이 남는다)
set -u
B=${1:-http://localhost:8091}
T=$(mktemp -d); A=$T/admin; U=$T/user; fail=0
ok()  { echo "  ok   $1"; }
bad() { echo "  FAIL $1"; fail=1; }
# check 이름 기대코드 실제코드
check() { [ "$2" = "$3" ] && ok "$1" || bad "$1 (expected $2, got $3)"; }
req() { local jar=$1 m=$2 url=$3 body=${4:-}; if [ -n "$body" ]; then curl -s -o $T/out -w '%{http_code}' -b $jar -c $jar -X $m -H 'Content-Type: application/json' -d "$body" "$B$url"; else curl -s -o $T/out -w '%{http_code}' -b $jar -c $jar -X $m "$B$url"; fi; }
json() { python3 -c "import json,sys; print(json.load(open('$T/out'))$1)"; }
ID="smoke$(date +%s | tail -c 7)"

echo "[로그인 전]"
check "피드 → 로그인으로" 302 "$(curl -s -o /dev/null -w '%{http_code}' $B/)"
check "API 401" 401 "$(req $U GET /api/shifts?month=2026-10)"
check "로그인 화면" 200 "$(req $U GET /login)"
check "robots" 200 "$(curl -s -o /dev/null -w '%{http_code}' $B/robots.txt)"
check "틀린 비밀번호" 400 "$(req $A POST /api/login '{"loginId":"admin","password":"wrong-password"}')"

echo "[관리자]"
J="{\"loginId\":\"admin\",\"password\":\"$ADMIN_PW\"}"
check "관리자 로그인" 200 "$(req $A POST /api/login "$J")"
check "글씨 크기 선택" 204 "$(req $A PUT /api/prefs '{"fontScale":"md"}')"
check "피드" 200 "$(req $A GET /)"

echo "[가입·승인]"
J="{\"loginId\":\"$ID\",\"password\":\"password123\",\"name\":\"동생\"}"
check "가입 신청" 200 "$(req $U POST /api/signup "$J")"
J="{\"loginId\":\"$ID\",\"password\":\"password123\",\"name\":\"x\"}"
check "중복 아이디" 400 "$(req $T/x POST /api/signup "$J")"
check "대기 중 피드 → 대기 화면" 302 "$(req $U GET /)"
check "대기 중 API 403" 403 "$(req $U GET /api/shifts?month=2026-10)"
UID_=$(sqlite3 data/family.db "SELECT id FROM member WHERE login_id='$ID'")
check "승인" 204 "$(req $A PUT /admin/api/members/$UID_/status '{"status":"ACTIVE"}')"
check "회원은 관리 화면 못 봄" 403 "$(req $U PUT /admin/api/members/1/status '{"status":"BLOCKED"}')"
check "첫 화면 → 글씨 크기" 302 "$(req $U GET /)"
req $U PUT /api/prefs '{"fontScale":"lg"}' >/dev/null

echo "[글]"
check "빈 글 거절" 400 "$(req $U POST /api/posts '{"kind":"moment","body":"","postDate":"2026-10-14","mediaIds":[]}')"
check "순간 올리기" 200 "$(req $U POST /api/posts '{"kind":"moment","body":"오늘 저녁","postDate":"2026-10-14","mediaIds":[]}')"; P1=$(json "['id']")
check "나만 보기 일기" 200 "$(req $U POST /api/posts '{"kind":"diary","title":"비밀","body":"혼자 쓰는 일기","visibility":"private","postDate":"2026-10-14","mediaIds":[]}')"; P2=$(json "['id']")
check "관리자도 나만 보기 못 봄" 404 "$(req $A GET /p/$P2)"
check "관리자 댓글" 200 "$(req $A POST /api/posts/$P1/comments '{"body":"맛있겠다"}')"
check "하트" 200 "$(req $A PUT /api/posts/$P1/reaction '{}')"; [ "$(json "['liked']")" = "True" ] && ok "하트 켜짐" || bad "하트 켜짐"
check "글 상세" 200 "$(req $U GET /p/$P1)"
check "남의 글 수정 불가" 403 "$(req $A PUT /api/posts/$P1 '{"kind":"moment","body":"x","postDate":"2026-10-14","mediaIds":[]}')"
check "남의 사진 붙이기 불가" 400 "$(req $A POST /api/posts '{"kind":"moment","body":"x","postDate":"2026-10-14","mediaIds":[999999]}')"
check "다른 사이트 Origin 거절" 403 "$(curl -s -o /dev/null -w '%{http_code}' -b $U -X POST -H 'Origin: https://evil.example' -H 'Content-Type: application/json' -d '{}' $B/api/posts)"
check "Origin 없는 form 전송 거절" 415 "$(curl -s -o /dev/null -w '%{http_code}' -b $U -X POST -d 'kind=moment' $B/api/posts)"

echo "[사진]"
SRC=$(ls /System/Library/Desktop\ Pictures/*.heic 2>/dev/null | head -1)
if [ -n "$SRC" ]; then
  sips -s format jpeg -Z 1200 "$SRC" --out $T/v.jpg >/dev/null 2>&1; sips -s format jpeg -Z 400 "$SRC" --out $T/t.jpg >/dev/null 2>&1
  cat $T/v.jpg $T/t.jpg $T/v.jpg > $T/body.bin
  VL=$(wc -c < $T/v.jpg | tr -d ' '); TL=$(wc -c < $T/t.jpg | tr -d ' ')
  check "사진 올리기" 200 "$(curl -s -o $T/out -w '%{http_code}' -b $U -X POST -H 'Content-Type: application/octet-stream' --data-binary @$T/body.bin "$B/api/media?view=$VL&thumb=$TL&takenAt=2026-10-10T10:00:00")"; M1=$(json "['id']")
  check "가짜 사진 거절" 400 "$(curl -s -o /dev/null -w '%{http_code}' -b $U -X POST -H 'Content-Type: application/octet-stream' --data-binary 'notanimage-notanimage' "$B/api/media?view=5&thumb=5")"
  J="{\"kind\":\"moment\",\"body\":\"\",\"postDate\":\"2026-10-10\",\"mediaIds\":[$M1]}"
  check "사진 글" 200 "$(req $U POST /api/posts "$J")"
  check "앨범" 200 "$(req $U GET /album)"
  C=$(req $U GET /api/media/$M1/original); [ "$C" = 200 ] || [ "$C" = 302 ] && ok "원본 받기(올린 사람, 로컬 200·Ncloud 302)" || bad "원본 받기 ($C)"
  check "원본 받기 기록" 1 "$(sqlite3 data/family.db "SELECT COUNT(*) FROM audit_log WHERE action='media.original.download' AND target='media:$M1'")"
  check "같은 사진 두 번 붙이기 불가" 400 "$(req $U POST /api/posts "$J")"
  ORIG=$(sqlite3 data/family.db "SELECT family_id || '/original/' || original FROM media WHERE id=$M1")
  check "원본은 /files 로 못 받음" 404 "$(req $U GET /files/$ORIG)"
else echo "  skip (테스트 사진 없음)"; fi

echo "[근무표]"
check "3교대 프리셋" 200 "$(req $U POST /api/shift-setting/preset '{"system":"3"}')"
check "3개월 채우기" 200 "$(req $U POST /api/shifts/fill '{"start":"2026-10-01","months":3}')"
N=$(sqlite3 data/family.db "SELECT COUNT(*) FROM shift_day WHERE member_id=$UID_")
check "채운 날 수(10~12월 92일)" 92 "$N"
FIRST=$(sqlite3 data/family.db "SELECT t.short FROM shift_day d JOIN shift_type t ON t.id=d.shift_type_id WHERE d.member_id=$UID_ AND d.day IN ('2026-10-01','2026-10-03','2026-10-05','2026-10-07','2026-10-09') ORDER BY d.day" | tr '\n' ' ')
check "순환(D E N 휴 D)" "D E N 휴 D " "$FIRST"
check "하루 바꾸기" 200 "$(req $U PUT /api/shifts/2026-10-14 '{"typeId":null,"note":"병원"}')"
check "남의 교대 종류 거절" 400 "$(req $A PUT /api/shifts/2026-10-15 '{"typeId":1}')"
J="{\"memberId\":$UID_,\"typeId\":null,\"note\":\"대신\"}"
check "관리자가 대신 입력" 200 "$(req $A PUT /api/shifts/2026-10-16 "$J")"
check "가족 보기" 200 "$(req $A GET '/api/shifts?month=2026-10&scope=family')"
check "캘린더 화면" 200 "$(req $U GET /calendar)"

echo "[비밀번호]"
check "초기화" 200 "$(req $A POST /admin/api/members/$UID_/reset-password '{}')"; TMP=$(json "['tempPassword']")
check "초기화 후 기존 세션 끊김" 302 "$(req $U GET /)"
J="{\"loginId\":\"$ID\",\"password\":\"$TMP\"}"
check "임시 비밀번호 로그인" 200 "$(req $U POST /api/login "$J")"
check "변경 전 피드 막힘" 302 "$(req $U GET /)"
J="{\"current\":\"$TMP\",\"next\":\"newpass1234\"}"
check "새 비밀번호" 204 "$(req $U PUT /api/me/password "$J")"
check "변경 후 피드" 200 "$(req $U GET /)"
check "차단" 204 "$(req $A PUT /admin/api/members/$UID_/status '{"status":"BLOCKED"}')"
J="{\"loginId\":\"$ID\",\"password\":\"newpass1234\"}"
check "차단된 계정 로그인 불가" 400 "$(req $T/y POST /api/login "$J")"

rm -rf $T
[ $fail = 0 ] && echo "ALL OK" || { echo "SOME FAILED"; exit 1; }
