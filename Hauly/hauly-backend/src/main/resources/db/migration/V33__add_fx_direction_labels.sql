-- =============================================================
-- V33__add_fx_direction_labels.sql
-- 재무 정보 모달의 환율 입력 방향 토글 라벨 (1바트당 원 ⇄ 1원당 바트).
-- =============================================================

INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    ('field.fx.dir.krw_per_thb', 'ko', '1바트당 KRW',  'admin', true),
    ('field.fx.dir.krw_per_thb', 'en', 'KRW per THB',  'admin', true),
    ('field.fx.dir.krw_per_thb', 'th', 'KRW ต่อ 1 THB', 'admin', true),

    ('field.fx.dir.thb_per_krw', 'ko', '1원당 THB',    'admin', true),
    ('field.fx.dir.thb_per_krw', 'en', 'THB per KRW',  'admin', true),
    ('field.fx.dir.thb_per_krw', 'th', 'THB ต่อ 1 KRW', 'admin', true);
