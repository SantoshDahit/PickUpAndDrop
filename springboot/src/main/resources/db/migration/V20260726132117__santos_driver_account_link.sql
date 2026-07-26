-- ============================================
-- Migration V20260726132117
-- ============================================
-- 설명: driver ↔ user 계정 연결 (plan 005) — 기사 로그인
-- 작성일: 2026-07-26 13:21:17
-- ============================================

ALTER TABLE driver ADD COLUMN user_id CHAR(36) NULL,
  ADD CONSTRAINT uk_driver_user UNIQUE (user_id),
  ADD CONSTRAINT fk_driver_user FOREIGN KEY (user_id) REFERENCES `user` (id);
