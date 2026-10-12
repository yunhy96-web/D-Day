#!/bin/sh
# 가장 작은 회귀 체크. 사용: scripts/smoke.sh [base-url]   (기본 http://localhost:8080)
U="${1:-http://localhost:8080}"; H='Content-Type: application/json'; SID=$(uuidgen | tr A-Z a-z); fail=0
chk() { [ "$2" = "$3" ] && echo "ok   $1" || { echo "FAIL $1: got '$2' want '$3'"; fail=1; }; }
code() { curl -s -m 20 -o /dev/null -w '%{http_code}' "$@"; }
for p in / /c/worldcup /c/guestbook /c/gallery /c/map /c/quiz /c/timeline /admin; do chk "GET $p" "$(code $U$p)" 200; done
chk "404 unknown content" "$(code $U/c/nope)" 404
chk "404 missing photo" "$(code $U/files/photos/view/none.jpg)" 404
chk "originals not served" "$(code $U/files/photos/original/x.jpg)" 404
chk "415 non-json api" "$(code -X POST $U/api/heartbeat -d x)" 415
chk "heartbeat" "$(curl -s -X POST $U/api/heartbeat -H "$H" -d "{\"sid\":\"$SID\"}" | grep -c online)" 1
chk "bad sid" "$(code -X POST $U/api/heartbeat -H "$H" -d '{"sid":"x"}')" 400
chk "pick bad photo" "$(code -X POST $U/api/pick -H "$H" -d "{\"sid\":\"$SID\",\"round\":5,\"winnerId\":\"zz\",\"loserId\":\"yy\"}")" 400
chk "event whitelist" "$(code -X POST $U/api/event -H "$H" -d "{\"sid\":\"$SID\",\"name\":\"evil\"}")" 400
R=$(curl -s -X POST $U/api/guestbook -H "$H" -d "{\"sid\":\"$SID\",\"name\":\"smoke\",\"message\":\"<b>x</b>\",\"password\":\"1234\"}"); ID=$(echo "$R" | sed 's/[^0-9]//g')
chk "guestbook write" "$(echo "$R" | grep -c id)" 1
chk "guestbook escaped in html" "$(curl -s $U/c/guestbook | grep -q "&lt;b&gt;x&lt;/b&gt;" && echo yes)" yes
chk "guestbook cooldown" "$(code -X POST $U/api/guestbook -H "$H" -d "{\"sid\":\"$SID\",\"name\":\"a\",\"message\":\"b\",\"password\":\"1234\"}")" 429
chk "delete wrong pw" "$(code -X DELETE $U/api/guestbook/$ID -H "$H" -d '{"password":"0000"}')" 403
chk "delete ok" "$(code -X DELETE $U/api/guestbook/$ID -H "$H" -d '{"password":"1234"}')" 204
chk "csp header" "$(curl -sI $U/ | grep -ci 'content-security-policy')" 1
chk "lang cookie th" "$(curl -s -b lang=th $U/ | grep -c '<html lang="th"')" 1
chk "admin api needs login" "$(code $U/admin/api/quiz)" 401
chk "admin upload needs login" "$(code -X POST "$U/admin/api/timeline?month=2025-05" -H 'Content-Type: image/jpeg' --data-binary x)" 401
chk "quiz bad level" "$(code -X POST $U/api/quiz/start -H "$H" -d "{\"sid\":\"$SID\",\"level\":\"x\"}")" 400
chk "quiz start no answer field" "$(curl -s -X POST $U/api/quiz/start -H "$H" -d "{\"sid\":\"$SID\",\"level\":\"easy\"}" | grep -ci answer)" 0
chk "quiz unknown attempt" "$(code -X POST $U/api/quiz/$(uuidgen)/submit -H "$H" -d '{"answers":[]}')" 409
chk "guestbook photo rejects non-image" "$(code -X POST $U/api/guestbook/photo -H 'Content-Type: image/jpeg' --data-binary 'notanimage')" 400
chk "admin photo upload needs login" "$(code -X POST "$U/admin/api/photos?view=1&thumb=1" -H 'Content-Type: application/octet-stream' --data-binary x)" 401
exit $fail
