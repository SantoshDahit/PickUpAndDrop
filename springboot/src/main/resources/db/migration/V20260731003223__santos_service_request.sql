-- ============================================
-- Migration V20260731003223
-- ============================================
-- 설명: 여행자 부가 서비스 요청 (plan 013) — 유심(SIM_CARD) 신청부터.
--       서비스별 테이블을 만들지 않고 type으로 구분한다: 다음 서비스가
--       같은 스키마/엔드포인트/관리자 큐를 재사용 (011의 email_verification과 동일 원칙).
--       type별 필드는 nullable — 서비스마다 필요한 항목이 다르다.
-- 작성일: 2026-07-31 00:32:23
-- ============================================

CREATE TABLE service_request (
    id           CHAR(36)      NOT NULL,
    user_id      CHAR(36)      NOT NULL,
    type         VARCHAR(20)   NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    -- type별 항목 (SIM_CARD 기준)
    arrival_date DATE          NULL,
    airport      VARCHAR(60)   NULL,
    detail       VARCHAR(120)  NULL,   -- 선택한 요금제
    deliver_to   VARCHAR(255)  NULL,
    contact      VARCHAR(60)   NULL,
    notes        VARCHAR(1000) NULL,   -- 여행자 메모
    admin_note   VARCHAR(1000) NULL,   -- 운영자 전용, 여행자에게 노출 안 함
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    deleted_at   DATETIME(6)   NULL,
    PRIMARY KEY (id),
    KEY idx_service_request_user (user_id, created_at),
    KEY idx_service_request_queue (type, status, arrival_date),
    CONSTRAINT fk_service_request_user
        FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
