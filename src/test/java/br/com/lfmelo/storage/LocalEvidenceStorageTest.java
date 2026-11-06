package br.com.lfmelo.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import javax.imageio.ImageIO;
import br.com.lfmelo.enums.EvidenceContentType;
import br.com.lfmelo.exception.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LocalEvidenceStorageTest {
    private final Path directory = Path.of("target", "evidence-test-" + UUID.randomUUID());
    private final LocalEvidenceStorage storage = new LocalEvidenceStorage(directory.toString());

    @Test
    void rejectsBlankStorageDirectory() {
        assertThrows(IllegalArgumentException.class, () -> new LocalEvidenceStorage("  "));
    }

    @AfterEach
    void cleanUp() throws Exception {
        if (Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    @Test
    void storesValidPngUnderGeneratedKeyAndLoadsIt() throws Exception {
        byte[] image = png();
        var stored = storage.store(new ByteArrayInputStream(image), EvidenceContentType.IMAGE_PNG, image.length);
        assertEquals(image.length, stored.sizeBytes());
        assertEquals(UUID.fromString(stored.key()).toString(), stored.key());
        assertArrayEquals(image, storage.load(stored.key()).getContentAsByteArray());
        storage.delete(stored.key());
        assertEquals(0, fileCount());
    }

    @Test
    void rejectsIncorrectContentAndCleansUp() throws Exception {
        assertThrows(BadRequestException.class, () -> storage.store(
                new ByteArrayInputStream(png()), EvidenceContentType.IMAGE_JPEG, 1_000_000));
        assertThrows(BadRequestException.class, () -> storage.store(
                new ByteArrayInputStream(new byte[] {1, 2, 3}), EvidenceContentType.IMAGE_HEIC, 100));
        assertThrows(BadRequestException.class, () -> storage.store(
                new ByteArrayInputStream(new byte[0]), EvidenceContentType.IMAGE_PNG, 100));
        assertEquals(0, fileCount());
    }

    @Test
    void rejectsOversizeStreamAndTraversal() throws Exception {
        assertThrows(EvidenceSizeExceededException.class, () -> storage.store(
                new ByteArrayInputStream(png()), EvidenceContentType.IMAGE_PNG, 4));
        assertThrows(EvidenceStorageException.class, () -> storage.load("../other"));
        assertThrows(EvidenceStorageException.class, () -> storage.delete("../other"));
        assertEquals(0, fileCount());
    }

    @Test
    void enforcesTenMebibyteLimitAtTheBoundary() throws Exception {
        int maximumBytes = 10 * 1024 * 1024;
        byte[] image = Arrays.copyOf(png(), maximumBytes + 1);
        var stored = storage.store(
                new ByteArrayInputStream(image, 0, maximumBytes), EvidenceContentType.IMAGE_PNG, maximumBytes);
        assertEquals(maximumBytes, stored.sizeBytes());
        assertThrows(EvidenceSizeExceededException.class, () -> storage.store(
                new ByteArrayInputStream(image), EvidenceContentType.IMAGE_PNG, maximumBytes));
        assertEquals(1, fileCount());
    }

    @Test
    void acceptsJpegAndWebpSignaturesWithConsistentPayload() throws Exception {
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpeg", jpeg);
        byte[] webp = Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
        assertEquals(EvidenceContentType.IMAGE_JPEG,
                storage.store(new ByteArrayInputStream(jpeg.toByteArray()), EvidenceContentType.IMAGE_JPEG, 1000)
                        .contentType());
        assertEquals(EvidenceContentType.IMAGE_WEBP,
                storage.store(new ByteArrayInputStream(webp), EvidenceContentType.IMAGE_WEBP, 1000).contentType());
    }

    @Test
    void cleansTemporaryFileIfUploadStreamFails() throws Exception {
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("broken stream");
            }
        };
        assertThrows(EvidenceStorageException.class, () -> storage.store(broken, EvidenceContentType.IMAGE_PNG, 100));
        assertEquals(0, fileCount());
    }

    private long fileCount() throws Exception {
        try (var files = Files.list(directory)) {
            return files.count();
        }
    }

    private static byte[] png() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
}
