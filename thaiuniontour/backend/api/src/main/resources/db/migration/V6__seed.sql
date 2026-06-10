-- V6: seed (공통코드/라벨 기본값)

-- 결제수단: 계좌이체만 활성, 간편결제/카드는 비활성(확장 대기)
INSERT INTO common.common_code(code_group, code, name, sort, use_yn) VALUES
  ('PAYMENT_METHOD','BANK_TRANSFER','계좌이체',1,true),
  ('PAYMENT_METHOD','NAVER_PAY','네이버페이',2,false),
  ('PAYMENT_METHOD','KAKAO_PAY','카카오페이',3,false),
  ('PAYMENT_METHOD','TOSS_PAY','토스페이',4,false),
  ('PAYMENT_METHOD','CARD','카드결제',5,false);

-- 다국어 라벨(ko/en) 예시
INSERT INTO common.label(code, locale, text) VALUES
  ('product.book','ko','예약하기'),
  ('product.book','en','Book Now'),
  ('search.placeholder','ko','어떤 여행을 찾으세요?'),
  ('search.placeholder','en','Where to?');
