-- ============================================
-- Migration V20260730005223
-- ============================================
-- 설명: 이메일 인증 코드 테이블 (JavaMail 연동) — 컨벤션 17의 sms_verification을
--       이메일 채널에 적용. purpose로 여러 흐름을 한 테이블에서 처리:
--       현재는 PASSWORD_RESET만 사용, 이후 계정 인증/탈퇴 확인 등이 재사용.
--       원문 코드는 저장하지 않고 SHA-256 해시만 보관, 1회용 + purpose별 만료.
-- 작성일: 2026-07-30 00:52:23
-- ============================================

CREATE TABLE email_verification (
    id          CHAR(36)     NOT NULL,
    -- 계정 생성 전 주소 인증도 가능하도록 NULL 허용
    user_id     CHAR(36)     NULL,
    contact     VARCHAR(254) NOT NULL,
    purpose     VARCHAR(20)  NOT NULL,
    -- SHA-256 hex: 테이블이 유출되어도 사용 가능한 링크가 나오지 않는다
    code_hash   CHAR(64)     NOT NULL,
    status      VARCHAR(10)  NOT NULL,
    verified_at DATETIME(6)  NULL,
    used_at     DATETIME(6)  NULL,
    expires_at  DATETIME(6)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_verification_code_hash (code_hash),
    KEY idx_email_verification_user_purpose (user_id, purpose, status),
    KEY idx_email_verification_contact (contact),
    CONSTRAINT fk_email_verification_user
        FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
