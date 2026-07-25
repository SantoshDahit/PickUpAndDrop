-- ============================================
-- Migration V20260726013740
-- ============================================
-- 설명: LandGreet 초기 스키마 (user, route, travel_group, booking, group_message) + 기본 route 시드
-- 작성일: 2026-07-26 01:37:40
-- ============================================

CREATE TABLE `user` (
  id         CHAR(36)     NOT NULL,
  email      VARCHAR(320) NOT NULL,
  password   VARCHAR(200) NOT NULL,
  name       VARCHAR(100) NOT NULL,
  phone      VARCHAR(30)  NULL,
  role       VARCHAR(20)  NOT NULL,
  created_at DATETIME(6)  NOT NULL,
  updated_at DATETIME(6)  NOT NULL,
  deleted_at DATETIME(6)  NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE route (
  id            CHAR(36)     NOT NULL,
  from_location VARCHAR(200) NOT NULL,
  to_location   VARCHAR(200) NOT NULL,
  active        BIT(1)       NOT NULL DEFAULT b'1',
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE travel_group (
  id         CHAR(36)    NOT NULL,
  route_id   CHAR(36)    NOT NULL,
  status     VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  KEY idx_travel_group_route_status (route_id, status),
  CONSTRAINT fk_travel_group_route FOREIGN KEY (route_id) REFERENCES route (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE booking (
  id          CHAR(36)      NOT NULL,
  user_id     CHAR(36)      NOT NULL,
  route_id    CHAR(36)      NOT NULL,
  group_id    CHAR(36)      NULL,
  travel_date DATE          NOT NULL,
  flight_no   VARCHAR(20)   NULL,
  party_size  INT           NOT NULL,
  match_pref  VARCHAR(20)   NOT NULL,
  intro       VARCHAR(300)  NULL,
  contact     VARCHAR(100)  NULL,
  notes       VARCHAR(1000) NULL,
  status      VARCHAR(20)   NOT NULL,
  created_at  DATETIME(6)   NOT NULL,
  updated_at  DATETIME(6)   NOT NULL,
  deleted_at  DATETIME(6)   NULL,
  PRIMARY KEY (id),
  KEY idx_booking_user (user_id),
  KEY idx_booking_group (group_id),
  CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES `user` (id),
  CONSTRAINT fk_booking_route FOREIGN KEY (route_id) REFERENCES route (id),
  CONSTRAINT fk_booking_group FOREIGN KEY (group_id) REFERENCES travel_group (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE group_message (
  id         CHAR(36)      NOT NULL,
  group_id   CHAR(36)      NOT NULL,
  user_id    CHAR(36)      NOT NULL,
  body       VARCHAR(1000) NOT NULL,
  created_at DATETIME(6)   NOT NULL,
  created_by VARCHAR(100)  NULL,
  PRIMARY KEY (id),
  KEY idx_group_message_group (group_id),
  CONSTRAINT fk_group_message_group FOREIGN KEY (group_id) REFERENCES travel_group (id),
  CONSTRAINT fk_group_message_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Reference data: starter routes
INSERT INTO route (id, from_location, to_location, active, created_at)
VALUES
  (UUID(), 'Incheon Airport (ICN)', 'Seoul', b'1', NOW(6)),
  (UUID(), 'Incheon Airport (ICN)', 'Daejeon', b'1', NOW(6));
