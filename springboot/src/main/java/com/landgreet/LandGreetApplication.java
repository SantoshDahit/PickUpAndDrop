package com.landgreet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LandGreetApplication {

    public static void main(String[] args) {
        ensureDbDirectory();
        SpringApplication.run(LandGreetApplication.class, args);
    }

    /** SQLite creates the db file but not its parent directory. */
    private static void ensureDbDirectory() {
        String dbPath = System.getenv().getOrDefault("APP_DB_PATH", "data/app-spring.db");
        Path parent = Path.of(dbPath).toAbsolutePath().getParent();
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create database directory " + parent, e);
        }
    }
}
