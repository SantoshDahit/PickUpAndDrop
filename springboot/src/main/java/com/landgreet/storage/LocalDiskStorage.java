package com.landgreet.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalDiskStorage implements ObjectStorage {

    private final Path baseDir;

    public LocalDiskStorage(@Value("${app.storage-dir}") String storageDir) {
        this.baseDir = Path.of(storageDir).toAbsolutePath().normalize();
    }

    @Override
    public void put(String key, byte[] data, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store object " + key, e);
        }
    }

    @Override
    public Optional<StoredObject> get(String key) {
        Path target = resolve(key);
        if (!Files.isRegularFile(target)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new StoredObject(Files.readAllBytes(target), contentTypeFor(key)));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read object " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete object " + key, e);
        }
    }

    private Path resolve(String key) {
        Path resolved = baseDir.resolve(key).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException("Object key escapes storage dir: " + key);
        }
        return resolved;
    }

    private static String contentTypeFor(String key) {
        return key.endsWith(".jpg") ? "image/jpeg" : "application/octet-stream";
    }
}
