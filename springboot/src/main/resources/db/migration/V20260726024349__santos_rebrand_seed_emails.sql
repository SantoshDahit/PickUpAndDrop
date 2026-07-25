-- ============================================
-- Migration V20260726024349
-- ============================================
-- 설명: 프로젝트명 변경 (LandGreet → Pickup&Drop) — 시드 계정 이메일 도메인 변경
-- 작성일: 2026-07-26 02:43:49
-- ============================================

UPDATE `user` SET email = 'admin@pickupdrop.com' WHERE email = 'admin@landgreet.com';
UPDATE `user` SET email = REPLACE(email, '@landgreet.dev', '@pickupdrop.dev') WHERE email LIKE '%@landgreet.dev';
