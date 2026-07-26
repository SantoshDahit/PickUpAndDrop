-- ============================================
-- Migration V20260726163711
-- ============================================
-- 설명: 가치 기반 단일 요금표 (plan 009 §0, 오너 지시) — 전 노선 공통
--       1인 150,000 / 2인 이상 1인당 140,000 (총액 = 150k×n − 10k×n, n≥2)
--       원가 분석은 내부 참고용으로 유지; 교통수단 선택은 운영 재량.
-- 작성일: 2026-07-26 16:37:11
-- ============================================

DELETE FROM price_tier;

INSERT INTO price_tier (id, route_id, group_size, price_per_person, created_at)
SELECT UUID(), r.id, t.group_size, t.price, NOW(6)
FROM route r
JOIN (
  SELECT 1 AS group_size, 150000 AS price UNION ALL
  SELECT 2, 140000
) t;
