-- 관리자 비밀번호를 화면에서 바꾸기(설계 §8). 행이 있으면 .env 의 ADMIN_PW_HASH 보다 우선한다.
CREATE TABLE admin_account (
  id            INTEGER PRIMARY KEY CHECK (id = 1),
  password_hash TEXT NOT NULL,
  updated_at    TEXT NOT NULL
);
