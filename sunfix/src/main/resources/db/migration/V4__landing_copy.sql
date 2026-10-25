-- 랜딩 문구 수정(R__labels 는 INSERT OR IGNORE 라 기존 키는 여기서 바꾼다)
UPDATE label SET text = '전화하기 전에, 업체별 가격부터 비교하세요. 연락처를 남기지 않아도 돼요.' WHERE key = 'home.sub' AND lang = 'ko';
UPDATE label SET text = 'See every vendor''s price before you call — no contact details required.' WHERE key = 'home.sub' AND lang = 'en';
