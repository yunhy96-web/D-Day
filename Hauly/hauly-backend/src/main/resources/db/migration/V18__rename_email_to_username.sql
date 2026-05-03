-- =============================================================
-- V18__rename_email_to_username.sql
-- 운영자 로그인을 이메일 형식에서 짧은 username 형식으로 변경.
-- (예: intake@hauly.local → intake). 컬럼명·길이만 정리하고 데이터는 보존.
-- 기존 사용자(admin/intake/buyer)는 이 단계에서 삭제하지 않음 — 운영 주문이 created_by 등으로
-- 참조 중이라 신규 계정(selim/union) 부트스트랩 후 별도 reassign+delete 단계에서 정리.
-- =============================================================

ALTER TABLE app_user RENAME COLUMN email TO username;
ALTER TABLE app_user ALTER COLUMN username TYPE VARCHAR(64);

-- UNIQUE 제약 자체는 컬럼명 변경에도 살아남지만, 인덱스 이름은 옛 컬럼명을 따라가므로 정리해 둔다.
ALTER INDEX app_user_email_key RENAME TO app_user_username_key;

-- 기존 이메일 형식 데이터를 local-part로 축약 (예: intake@hauly.local → intake)
UPDATE app_user
SET username = split_part(username, '@', 1)
WHERE username LIKE '%@%';

-- ----- i18n 라벨 (ko/en/th) -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('field.username.label',       'ko', '아이디',           'admin', true),
    ('field.username.label',       'en', 'Username',          'admin', true),
    ('field.username.label',       'th', 'ชื่อผู้ใช้',           'admin', true),

    ('field.username.placeholder', 'ko', '아이디 입력',        'admin', true),
    ('field.username.placeholder', 'en', 'Enter username',    'admin', true),
    ('field.username.placeholder', 'th', 'ป้อนชื่อผู้ใช้',         'admin', true),

    ('msg.error.username.invalid', 'ko', '아이디는 영문/숫자/언더스코어/하이픈만 사용해 3~32자로 입력하세요.', 'admin', true),
    ('msg.error.username.invalid', 'en', 'Username must be 3-32 characters of letters, numbers, underscore, or hyphen.', 'admin', true),
    ('msg.error.username.invalid', 'th', 'ชื่อผู้ใช้ต้องเป็นตัวอักษร/ตัวเลข/_/- ความยาว 3-32 ตัว', 'admin', true);
