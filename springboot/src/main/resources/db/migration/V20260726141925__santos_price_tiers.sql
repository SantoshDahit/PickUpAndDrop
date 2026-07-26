-- ============================================
-- Migration V20260726141925
-- ============================================
-- 설명: 그룹 규모별 1인 요금 (plan 008) — 프로토타입 요금표 시드
-- 작성일: 2026-07-26 14:19:25
-- ============================================

CREATE TABLE price_tier (
  id               CHAR(36)    NOT NULL,
  route_id         CHAR(36)    NOT NULL,
  group_size       INT         NOT NULL,
  price_per_person INT         NOT NULL,
  created_at       DATETIME(6) NOT NULL,
  updated_at       DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_price_tier (route_id, group_size),
  CONSTRAINT fk_price_tier_route FOREIGN KEY (route_id) REFERENCES route (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO price_tier (id, route_id, group_size, price_per_person, created_at)
SELECT UUID(), r.id, t.group_size, t.price, NOW(6)
FROM route r
JOIN (
  SELECT 'Seoul' AS dest, 1 AS group_size, 25000 AS price UNION ALL
  SELECT 'Seoul', 2, 20000 UNION ALL SELECT 'Seoul', 3, 20000 UNION ALL
  SELECT 'Seoul', 4, 16000 UNION ALL SELECT 'Seoul', 5, 14000 UNION ALL
  SELECT 'Seoul', 6, 12500 UNION ALL
  SELECT 'Daejeon', 1, 40000 UNION ALL SELECT 'Daejeon', 2, 32000 UNION ALL
  SELECT 'Daejeon', 3, 28000 UNION ALL SELECT 'Daejeon', 4, 24000 UNION ALL
  SELECT 'Daejeon', 5, 21000 UNION ALL SELECT 'Daejeon', 6, 19000
) t ON t.dest = r.to_location;
