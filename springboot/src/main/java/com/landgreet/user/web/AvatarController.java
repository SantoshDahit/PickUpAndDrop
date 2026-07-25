package com.landgreet.user.web;

import com.landgreet.storage.ObjectStorage;
import java.time.Duration;
import java.util.regex.Pattern;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AvatarController {

    /** UUID.jpg only — anything else (traversal attempts included) is a 404. */
    private static final Pattern KEY = Pattern.compile("^[0-9a-f\\-]{36}\\.jpg$");

    private final ObjectStorage storage;

    public AvatarController(ObjectStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/avatars/{file}")
    public ResponseEntity<byte[]> avatar(@PathVariable String file) {
        if (!KEY.matcher(file).matches()) {
            return ResponseEntity.notFound().build();
        }
        return storage.get("avatars/" + file)
                .map(object -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        // Safe to cache forever: every upload mints a new key.
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .body(object.data()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
