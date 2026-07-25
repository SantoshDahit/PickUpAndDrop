-- ============================================
-- Migration V20260726015914
-- ============================================
-- 설명: 개발/데모용 시드 계정 — 관리자 1명 + 일반 사용자 5명
--       비밀번호: admin@landgreet.com = admin123, 나머지 = password1
--       INSERT IGNORE: 기존 DB(이미 admin 존재)에서도 안전하게 적용
-- 작성일: 2026-07-26 01:59:14
-- ============================================

INSERT IGNORE INTO `user` (id, email, password, name, phone, role, created_at, updated_at)
VALUES
  (UUID(), 'admin@landgreet.com', '$2a$10$8xYAOVU0Pqve1E3USIsNA.y2oSJAks6bhQLF/Lon0ab6yoWXnd9ii', 'Admin', NULL, 'ADMIN', NOW(6), NOW(6)),
  (UUID(), 'minsu@landgreet.dev', '$2a$10$s29lpDMOwJIrrvZ2T8g1Q.lnHgYmUoIjwcknwTutpc5XhNchfTqSC', 'Minsu Park', '+82 10-1111-0001', 'USER', NOW(6), NOW(6)),
  (UUID(), 'sofia@landgreet.dev', '$2a$10$s29lpDMOwJIrrvZ2T8g1Q.lnHgYmUoIjwcknwTutpc5XhNchfTqSC', 'Sofia Rossi', '+82 10-1111-0002', 'USER', NOW(6), NOW(6)),
  (UUID(), 'david@landgreet.dev', '$2a$10$s29lpDMOwJIrrvZ2T8g1Q.lnHgYmUoIjwcknwTutpc5XhNchfTqSC', 'David Chen', '+82 10-1111-0003', 'USER', NOW(6), NOW(6)),
  (UUID(), 'amara@landgreet.dev', '$2a$10$s29lpDMOwJIrrvZ2T8g1Q.lnHgYmUoIjwcknwTutpc5XhNchfTqSC', 'Amara Okafor', '+82 10-1111-0004', 'USER', NOW(6), NOW(6)),
  (UUID(), 'lucas@landgreet.dev', '$2a$10$s29lpDMOwJIrrvZ2T8g1Q.lnHgYmUoIjwcknwTutpc5XhNchfTqSC', 'Lucas Müller', '+82 10-1111-0005', 'USER', NOW(6), NOW(6));
