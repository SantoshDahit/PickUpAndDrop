package com.landgreet.storage;

import java.util.Optional;

/**
 * Minimal object-store abstraction. The DB stores opaque keys; swapping the
 * local-disk driver for S3/R2 later must not touch any call site.
 */
public interface ObjectStorage {

    void put(String key, byte[] data, String contentType);

    Optional<StoredObject> get(String key);

    void delete(String key);
}
