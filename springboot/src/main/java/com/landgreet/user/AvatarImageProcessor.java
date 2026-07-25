package com.landgreet.user;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Component;

/**
 * Every upload is decoded and re-encoded — we never store bytes the user
 * sent. A failed decode IS the MIME validation; client content type and
 * filename are ignored entirely. Re-encoding applies EXIF orientation and
 * strips all metadata (GPS etc.).
 */
@Component
public class AvatarImageProcessor {

    public static final int SIZE = 256;

    public byte[] toSquareJpeg(InputStream input) {
        var out = new ByteArrayOutputStream();
        try {
            Thumbnails.of(input)
                    .size(SIZE, SIZE)
                    .crop(Positions.CENTER)
                    .imageType(BufferedImage.TYPE_INT_RGB) // flatten alpha for JPEG
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(out);
        } catch (IOException | IllegalArgumentException e) {
            throw new InvalidImageException("That doesn't look like an image we can read.", e);
        }
        return out.toByteArray();
    }
}
