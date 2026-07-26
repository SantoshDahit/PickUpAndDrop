-- ============================================
-- Migration V20260726160310
-- ============================================
-- 설명: 랜딩 주(週) 버킷 그룹 (plan 008) — travel_group.week_bucket 추가 + 백필
--       버킷: 1-7 / 8-14 / 15-21 / 22-28 / 29-말일 (월별 5주)
-- 작성일: 2026-07-26 16:03:10
-- ============================================

ALTER TABLE travel_group ADD COLUMN week_bucket VARCHAR(12) NULL;

-- published rides: from the advertised target date
UPDATE travel_group
SET week_bucket = CONCAT(DATE_FORMAT(target_date, '%Y-%m'), '-W',
                         LEAST(4, FLOOR((DAY(target_date) - 1) / 7)) + 1)
WHERE target_date IS NOT NULL;

-- organic groups: from the earliest member booking date
UPDATE travel_group tg
SET week_bucket = (
  SELECT CONCAT(DATE_FORMAT(MIN(b.travel_date), '%Y-%m'), '-W',
                LEAST(4, FLOOR((DAY(MIN(b.travel_date)) - 1) / 7)) + 1)
  FROM booking b WHERE b.group_id = tg.id
)
WHERE tg.week_bucket IS NULL
  AND EXISTS (SELECT 1 FROM booking b2 WHERE b2.group_id = tg.id);
