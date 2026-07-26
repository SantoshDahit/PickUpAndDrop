-- ============================================
-- Migration V20260726155406
-- ============================================
-- 설명: 한국 주요 도시 노선 시드 — ICN 공항발, 거리 기반 프로토타입 요금표 포함.
--       모든 INSERT는 NOT EXISTS 가드 — 관리자가 콘솔에서 수정/삭제한 내용을
--       덮어쓰지 않으며, 기존 노선(Seoul/Daejeon)은 건드리지 않는다.
--       (노선은 /v1/admin/routes PATCH로 언제든 수정 가능)
-- 작성일: 2026-07-26 15:54:06
-- ============================================

-- Fresh installs: init 시드의 from_location을 현재 브랜딩에 맞춘다
-- (기존 운영 DB에서는 이미 API로 변경돼 매칭 행이 없어 no-op).
UPDATE route SET from_location = 'ICN Airport' WHERE from_location = 'Incheon Airport (ICN)';

-- 노선: 목적지당 1개, 이미 같은 목적지 노선이 있으면 건너뛴다.
INSERT INTO route (id, from_location, to_location, active, created_at)
SELECT UUID(), 'ICN Airport', d.dest, b'1', NOW(6)
FROM (
  SELECT 'Incheon City' AS dest UNION ALL
  SELECT 'Suwon'        UNION ALL
  SELECT 'Pyeongtaek'   UNION ALL
  SELECT 'Cheonan'      UNION ALL
  SELECT 'Chuncheon'    UNION ALL
  SELECT 'Sejong'       UNION ALL
  SELECT 'Cheongju'     UNION ALL
  SELECT 'Gangneung'    UNION ALL
  SELECT 'Jeonju'       UNION ALL
  SELECT 'Daegu'        UNION ALL
  SELECT 'Gwangju'      UNION ALL
  SELECT 'Ulsan'        UNION ALL
  SELECT 'Busan'
) d
WHERE NOT EXISTS (SELECT 1 FROM route r WHERE r.to_location = d.dest);

-- 요금표: (노선, 인원) 조합이 없을 때만 삽입 — 거리 기반 프로토타입 요금.
INSERT INTO price_tier (id, route_id, group_size, price_per_person, created_at)
SELECT UUID(), r.id, t.group_size, t.price, NOW(6)
FROM route r
JOIN (
  -- Incheon City (~20km)
  SELECT 'Incheon City' AS dest, 1 AS group_size, 20000 AS price UNION ALL
  SELECT 'Incheon City', 2, 16000 UNION ALL SELECT 'Incheon City', 3, 14000 UNION ALL
  SELECT 'Incheon City', 4, 12000 UNION ALL SELECT 'Incheon City', 5, 11000 UNION ALL
  SELECT 'Incheon City', 6, 10000 UNION ALL
  -- Suwon (~60km)
  SELECT 'Suwon', 1, 28000 UNION ALL SELECT 'Suwon', 2, 22000 UNION ALL
  SELECT 'Suwon', 3, 20000 UNION ALL SELECT 'Suwon', 4, 17000 UNION ALL
  SELECT 'Suwon', 5, 15000 UNION ALL SELECT 'Suwon', 6, 14000 UNION ALL
  -- Pyeongtaek (~85km)
  SELECT 'Pyeongtaek', 1, 32000 UNION ALL SELECT 'Pyeongtaek', 2, 26000 UNION ALL
  SELECT 'Pyeongtaek', 3, 23000 UNION ALL SELECT 'Pyeongtaek', 4, 20000 UNION ALL
  SELECT 'Pyeongtaek', 5, 18000 UNION ALL SELECT 'Pyeongtaek', 6, 16000 UNION ALL
  -- Cheonan (~100km)
  SELECT 'Cheonan', 1, 35000 UNION ALL SELECT 'Cheonan', 2, 28000 UNION ALL
  SELECT 'Cheonan', 3, 25000 UNION ALL SELECT 'Cheonan', 4, 22000 UNION ALL
  SELECT 'Cheonan', 5, 19000 UNION ALL SELECT 'Cheonan', 6, 17500 UNION ALL
  -- Chuncheon (~130km)
  SELECT 'Chuncheon', 1, 38000 UNION ALL SELECT 'Chuncheon', 2, 31000 UNION ALL
  SELECT 'Chuncheon', 3, 27000 UNION ALL SELECT 'Chuncheon', 4, 24000 UNION ALL
  SELECT 'Chuncheon', 5, 21000 UNION ALL SELECT 'Chuncheon', 6, 19000 UNION ALL
  -- Sejong (~140km)
  SELECT 'Sejong', 1, 40000 UNION ALL SELECT 'Sejong', 2, 32000 UNION ALL
  SELECT 'Sejong', 3, 28000 UNION ALL SELECT 'Sejong', 4, 24000 UNION ALL
  SELECT 'Sejong', 5, 21000 UNION ALL SELECT 'Sejong', 6, 19000 UNION ALL
  -- Cheongju (~150km)
  SELECT 'Cheongju', 1, 41000 UNION ALL SELECT 'Cheongju', 2, 33000 UNION ALL
  SELECT 'Cheongju', 3, 29000 UNION ALL SELECT 'Cheongju', 4, 25000 UNION ALL
  SELECT 'Cheongju', 5, 22000 UNION ALL SELECT 'Cheongju', 6, 20000 UNION ALL
  -- Gangneung (~230km)
  SELECT 'Gangneung', 1, 52000 UNION ALL SELECT 'Gangneung', 2, 42000 UNION ALL
  SELECT 'Gangneung', 3, 37000 UNION ALL SELECT 'Gangneung', 4, 32000 UNION ALL
  SELECT 'Gangneung', 5, 28000 UNION ALL SELECT 'Gangneung', 6, 25000 UNION ALL
  -- Jeonju (~230km)
  SELECT 'Jeonju', 1, 52000 UNION ALL SELECT 'Jeonju', 2, 42000 UNION ALL
  SELECT 'Jeonju', 3, 37000 UNION ALL SELECT 'Jeonju', 4, 32000 UNION ALL
  SELECT 'Jeonju', 5, 28000 UNION ALL SELECT 'Jeonju', 6, 25000 UNION ALL
  -- Daegu (~290km)
  SELECT 'Daegu', 1, 60000 UNION ALL SELECT 'Daegu', 2, 48000 UNION ALL
  SELECT 'Daegu', 3, 42000 UNION ALL SELECT 'Daegu', 4, 36000 UNION ALL
  SELECT 'Daegu', 5, 32000 UNION ALL SELECT 'Daegu', 6, 29000 UNION ALL
  -- Gwangju (~300km)
  SELECT 'Gwangju', 1, 62000 UNION ALL SELECT 'Gwangju', 2, 50000 UNION ALL
  SELECT 'Gwangju', 3, 43000 UNION ALL SELECT 'Gwangju', 4, 37000 UNION ALL
  SELECT 'Gwangju', 5, 33000 UNION ALL SELECT 'Gwangju', 6, 30000 UNION ALL
  -- Ulsan (~360km)
  SELECT 'Ulsan', 1, 70000 UNION ALL SELECT 'Ulsan', 2, 56000 UNION ALL
  SELECT 'Ulsan', 3, 49000 UNION ALL SELECT 'Ulsan', 4, 42000 UNION ALL
  SELECT 'Ulsan', 5, 37000 UNION ALL SELECT 'Ulsan', 6, 34000 UNION ALL
  -- Busan (~400km)
  SELECT 'Busan', 1, 75000 UNION ALL SELECT 'Busan', 2, 60000 UNION ALL
  SELECT 'Busan', 3, 52000 UNION ALL SELECT 'Busan', 4, 45000 UNION ALL
  SELECT 'Busan', 5, 40000 UNION ALL SELECT 'Busan', 6, 36000
) t ON t.dest = r.to_location
WHERE NOT EXISTS (
  SELECT 1 FROM price_tier p WHERE p.route_id = r.id AND p.group_size = t.group_size
);
