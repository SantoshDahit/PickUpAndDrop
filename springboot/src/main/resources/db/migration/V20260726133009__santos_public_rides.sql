-- ============================================
-- Migration V20260726133009
-- ============================================
-- 설명: 관리자 발행 라이드 (plan 006) — travel_group에 is_public / target_date 추가
-- 작성일: 2026-07-26 13:30:09
-- ============================================

ALTER TABLE travel_group
  ADD COLUMN is_public BIT(1) NOT NULL DEFAULT b'0',
  ADD COLUMN target_date DATE NULL;
