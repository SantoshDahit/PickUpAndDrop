-- ============================================
-- Migration V20260726162800
-- ============================================
-- 설명: 실비 기반 요금표 (plan 009) — 2-존 모델로 전체 요금 교체
--       Zone A(서울권: Seoul, Incheon City): 대중교통 동행 1-3인 / 4인+ 전용밴 15만
--       Zone B(그 외 전국): 시외버스 4만/인 동행 / 4인+ 차량 대절 20만
--       그리터 1일 인건비 12만원 반영. 노선별 미세조정은 관리자 Routes 페이지에서.
-- 작성일: 2026-07-26 16:28:00
-- ============================================

DELETE FROM price_tier;

-- Zone A: Seoul metro
INSERT INTO price_tier (id, route_id, group_size, price_per_person, created_at)
SELECT UUID(), r.id, t.group_size, t.price, NOW(6)
FROM route r
JOIN (
  SELECT 1 AS group_size, 150000 AS price UNION ALL
  SELECT 2,  90000 UNION ALL
  SELECT 3,  75000 UNION ALL
  SELECT 4,  72000 UNION ALL
  SELECT 5,  62000 UNION ALL
  SELECT 6,  55000
) t
WHERE r.to_location IN ('Seoul', 'Incheon City');

-- Zone B: rest of Korea
INSERT INTO price_tier (id, route_id, group_size, price_per_person, created_at)
SELECT UUID(), r.id, t.group_size, t.price, NOW(6)
FROM route r
JOIN (
  SELECT 1 AS group_size, 210000 AS price UNION ALL
  SELECT 2, 130000 UNION ALL
  SELECT 3, 105000 UNION ALL
  SELECT 4,  95000 UNION ALL
  SELECT 5,  80000 UNION ALL
  SELECT 6,  70000
) t
WHERE r.to_location NOT IN ('Seoul', 'Incheon City');
