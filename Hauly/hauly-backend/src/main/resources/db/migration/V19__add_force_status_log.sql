-- =============================================================
-- V19__add_force_status_log.sql
-- 상태 강제 변경 (state machine 우회) 기록을 위한 forced 컬럼 추가.
-- ADMIN이 사유와 함께 임의 상태로 되돌릴 수 있게 한 기능. 기록은 기존
-- order_status_log 그대로 사용하되 forced=true 로 구분한다.
-- =============================================================

ALTER TABLE order_status_log
    ADD COLUMN forced BOOLEAN NOT NULL DEFAULT FALSE;

-- ----- i18n 라벨 (ko/en/th) -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    -- 버튼/모달
    ('order.status.force.button',   'ko', '강제 변경',                        'admin', true),
    ('order.status.force.button',   'en', 'Force change',                      'admin', true),
    ('order.status.force.button',   'th', 'บังคับเปลี่ยน',                       'admin', true),

    ('order.status.force.title',    'ko', '상태 강제 변경 (관리자)',           'admin', true),
    ('order.status.force.title',    'en', 'Force status change (admin)',       'admin', true),
    ('order.status.force.title',    'th', 'บังคับเปลี่ยนสถานะ (แอดมิน)',          'admin', true),

    ('order.status.force.help',     'ko', '정상 흐름을 우회하여 임의 상태로 이동합니다. 사유는 필수입니다.',
                                      'admin', true),
    ('order.status.force.help',     'en', 'Bypasses the normal flow and moves to any status. Reason is required.',
                                      'admin', true),
    ('order.status.force.help',     'th', 'ข้ามขั้นตอนปกติเพื่อเปลี่ยนสถานะ ต้องระบุเหตุผล',
                                      'admin', true),

    ('order.status.force.target',   'ko', '이동할 상태',                       'admin', true),
    ('order.status.force.target',   'en', 'Target status',                     'admin', true),
    ('order.status.force.target',   'th', 'สถานะเป้าหมาย',                       'admin', true),

    ('order.status.force.reason',   'ko', '사유 (필수)',                       'admin', true),
    ('order.status.force.reason',   'en', 'Reason (required)',                 'admin', true),
    ('order.status.force.reason',   'th', 'เหตุผล (จำเป็น)',                       'admin', true),

    ('order.status.force.confirm',  'ko', '강제 변경',                        'admin', true),
    ('order.status.force.confirm',  'en', 'Force',                             'admin', true),
    ('order.status.force.confirm',  'th', 'บังคับ',                            'admin', true),

    -- 이력 표시 — 강제 변경 배지
    ('order.history.forced_badge',  'ko', '강제',                             'admin', true),
    ('order.history.forced_badge',  'en', 'forced',                            'admin', true),
    ('order.history.forced_badge',  'th', 'บังคับ',                            'admin', true),

    -- 에러
    ('msg.error.force_reason_required', 'ko', '강제 변경 사유는 필수입니다.',     'admin', true),
    ('msg.error.force_reason_required', 'en', 'Reason is required for forced changes.', 'admin', true),
    ('msg.error.force_reason_required', 'th', 'ต้องระบุเหตุผลในการบังคับเปลี่ยน',     'admin', true),

    ('msg.error.force_same_status', 'ko', '현재 상태와 동일한 상태로는 변경할 수 없습니다.',
                                      'admin', true),
    ('msg.error.force_same_status', 'en', 'Target status must differ from the current status.',
                                      'admin', true),
    ('msg.error.force_same_status', 'th', 'สถานะเป้าหมายต้องต่างจากสถานะปัจจุบัน',
                                      'admin', true);
