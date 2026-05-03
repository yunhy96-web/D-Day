-- =============================================================
-- V17__add_order_note.sql
-- 주문별 협업 메모 테이블. 운영자(나/여친)가 서로 코멘트 남길 수 있는 용도.
-- 본인 작성한 메모만 수정/삭제 가능하며, 삭제는 soft-delete (감사 추적용).
-- =============================================================

CREATE TABLE order_note (
    id          BIGSERIAL    PRIMARY KEY,
    order_id    BIGINT       NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    author_id   BIGINT       NOT NULL REFERENCES app_user(id),
    body        TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

-- 주문 상세 조회 시 메모 목록을 created_at DESC 로 빠르게 가져오기 위한 인덱스.
-- soft-delete된 행은 partial index 로 제외.
CREATE INDEX idx_order_note_order_active
    ON order_note (order_id, created_at DESC)
    WHERE deleted_at IS NULL;


-- ----- i18n 라벨 (ko/en/th) -----
INSERT INTO i18n_message (message_key, lang_code, message, context, is_system) VALUES
    -- 카드/섹션 타이틀
    ('order.detail.notes.title',        'ko', '메모',                            'admin', true),
    ('order.detail.notes.title',        'en', 'Notes',                           'admin', true),
    ('order.detail.notes.title',        'th', 'บันทึก',                            'admin', true),

    -- 입력 placeholder
    ('order.notes.placeholder',         'ko', '메모를 입력하세요',                  'admin', true),
    ('order.notes.placeholder',         'en', 'Write a note',                    'admin', true),
    ('order.notes.placeholder',         'th', 'พิมพ์บันทึก',                        'admin', true),

    -- 비어있을 때 안내
    ('order.notes.empty',               'ko', '아직 작성된 메모가 없습니다.',         'admin', true),
    ('order.notes.empty',               'en', 'No notes yet.',                   'admin', true),
    ('order.notes.empty',               'th', 'ยังไม่มีบันทึก',                      'admin', true),

    -- 편집됨 표시
    ('order.notes.edited',              'ko', '편집됨',                          'admin', true),
    ('order.notes.edited',              'en', 'edited',                          'admin', true),
    ('order.notes.edited',              'th', 'แก้ไขแล้ว',                          'admin', true),

    -- 액션 버튼
    ('order.notes.add',                 'ko', '메모 추가',                        'admin', true),
    ('order.notes.add',                 'en', 'Add note',                        'admin', true),
    ('order.notes.add',                 'th', 'เพิ่มบันทึก',                        'admin', true),

    ('order.notes.save',                'ko', '저장',                            'admin', true),
    ('order.notes.save',                'en', 'Save',                            'admin', true),
    ('order.notes.save',                'th', 'บันทึก',                           'admin', true),

    ('order.notes.cancel',              'ko', '취소',                            'admin', true),
    ('order.notes.cancel',              'en', 'Cancel',                          'admin', true),
    ('order.notes.cancel',              'th', 'ยกเลิก',                          'admin', true),

    ('order.notes.edit',                'ko', '편집',                            'admin', true),
    ('order.notes.edit',                'en', 'Edit',                            'admin', true),
    ('order.notes.edit',                'th', 'แก้ไข',                           'admin', true),

    ('order.notes.delete',              'ko', '삭제',                            'admin', true),
    ('order.notes.delete',              'en', 'Delete',                          'admin', true),
    ('order.notes.delete',              'th', 'ลบ',                              'admin', true),

    -- 삭제 확인
    ('order.notes.delete.confirm',      'ko', '이 메모를 삭제하시겠습니까?',           'admin', true),
    ('order.notes.delete.confirm',      'en', 'Delete this note?',               'admin', true),
    ('order.notes.delete.confirm',      'th', 'ลบบันทึกนี้หรือไม่?',                  'admin', true),

    -- 에러 메시지
    ('order.notes.error.empty',         'ko', '메모 내용을 입력하세요.',              'admin', true),
    ('order.notes.error.empty',         'en', 'Please enter a note.',            'admin', true),
    ('order.notes.error.empty',         'th', 'กรุณาพิมพ์บันทึก',                    'admin', true),

    ('order.notes.error.too_long',      'ko', '메모는 최대 2000자까지 입력 가능합니다.', 'admin', true),
    ('order.notes.error.too_long',      'en', 'Notes can be up to 2000 characters.', 'admin', true),
    ('order.notes.error.too_long',      'th', 'บันทึกได้สูงสุด 2000 ตัวอักษร',          'admin', true),

    ('order.notes.error.forbidden',     'ko', '본인이 작성한 메모만 편집/삭제할 수 있습니다.', 'admin', true),
    ('order.notes.error.forbidden',     'en', 'You can only edit or delete your own notes.', 'admin', true),
    ('order.notes.error.forbidden',     'th', 'คุณแก้ไขหรือลบได้เฉพาะบันทึกของคุณเท่านั้น', 'admin', true);
