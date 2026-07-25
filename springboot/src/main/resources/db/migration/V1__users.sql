CREATE TABLE users (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  name          TEXT    NOT NULL,
  email         TEXT    NOT NULL UNIQUE, -- stored lowercase; service normalises
  password_hash TEXT    NOT NULL,
  phone         TEXT,
  is_admin      INTEGER NOT NULL DEFAULT 0,
  avatar_key    TEXT,                    -- opaque storage key, never a path/URL
  created_at    TEXT    NOT NULL DEFAULT (datetime('now')),
  updated_at    TEXT,
  deleted_at    TEXT                     -- soft delete; NULL = active
);
