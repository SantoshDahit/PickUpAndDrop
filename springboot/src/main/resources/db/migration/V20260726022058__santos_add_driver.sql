-- ============================================
-- Migration V20260726022058
-- ============================================
-- 설명: driver 테이블 추가 + travel_group / booking에 driver_id 배정 컬럼 (plan 003)
-- 작성일: 2026-07-26 02:20:58
-- ============================================

CREATE TABLE driver (
  id           CHAR(36)     NOT NULL,
  name         VARCHAR(100) NOT NULL,
  phone        VARCHAR(30)  NULL,
  license_no   VARCHAR(50)  NULL,
  owns_vehicle BIT(1)       NOT NULL DEFAULT b'1',
  vehicle      VARCHAR(100) NULL,
  plate_no     VARCHAR(20)  NULL,
  seats        INT          NOT NULL DEFAULT 4,
  status       VARCHAR(20)  NOT NULL,
  created_at   DATETIME(6)  NOT NULL,
  updated_at   DATETIME(6)  NOT NULL,
  deleted_at   DATETIME(6)  NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE travel_group ADD COLUMN driver_id CHAR(36) NULL,
  ADD CONSTRAINT fk_travel_group_driver FOREIGN KEY (driver_id) REFERENCES driver (id);

ALTER TABLE booking ADD COLUMN driver_id CHAR(36) NULL,
  ADD CONSTRAINT fk_booking_driver FOREIGN KEY (driver_id) REFERENCES driver (id);
