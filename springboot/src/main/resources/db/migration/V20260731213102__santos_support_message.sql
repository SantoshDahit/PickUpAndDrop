-- ============================================
-- Migration V20260731213102
-- ============================================
-- 설명: 여행자 ↔ 운영자 1:1 문의 채팅 (plan 014).
--       그룹 채팅(group_message)은 그룹이 있어야 하므로 개별 이동 여행자는
--       연락 수단이 없었다. 스레드는 여행자 1명당 1개 — 별도 테이블 없이
--       user_id로 묶는다. staff는 작성 시점에 확정 (012와 동일 원칙).
-- 작성일: 2026-07-31 21:31:02
-- ============================================

CREATE TABLE support_message (
    id         CHAR(36)      NOT NULL,
    -- 스레드 주인 (문의한 여행자). 운영자 답장도 같은 user_id에 달린다
    user_id    CHAR(36)      NOT NULL,
    -- 실제 작성자: 여행자 본인이거나 답장한 관리자
    author_id  CHAR(36)      NOT NULL,
    staff      BOOLEAN       NOT NULL DEFAULT FALSE,
    body       VARCHAR(1000) NOT NULL,
    -- 상대방이 읽은 시각. 여행자 메시지는 운영자가, 반대는 여행자가 읽는다
    read_at    DATETIME(6)   NULL,
    created_at DATETIME(6)   NOT NULL,
    -- BaseCreateEntity(컨벤션 03)는 created_by도 매핑한다 — group_message와 동일
    created_by VARCHAR(100)  NULL,
    PRIMARY KEY (id),
    KEY idx_support_message_thread (user_id, created_at),
    KEY idx_support_message_unread (user_id, staff, read_at),
    CONSTRAINT fk_support_message_user
        FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_support_message_author
        FOREIGN KEY (author_id) REFERENCES `user` (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
