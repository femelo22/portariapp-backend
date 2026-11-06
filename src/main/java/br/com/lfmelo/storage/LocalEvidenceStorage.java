package br.com.lfmelo.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import br.com.lfmelo.enums.EvidenceContentType;
import br.com.lfmelo.exception.BadRequestException;
import br.com.lfmelo.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class LocalEvidenceStorage implements EvidenceStorage {
    private static final Logger log = LoggerFactory.getLogger(LocalEvidenceStorage.class);
    private final Path root;

    public LocalEvidenceStorage(@Value("${evidences.storage-directory:./data/evidences}") String directory) {
        if (directory == null || directory.isBlank()) {
            throw new IllegalArgumentException("Evidence storage directory must not be blank");
        }
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public StoredEvidence store(InputStream input, EvidenceContentType declaredType, long maximumBytes) {
        Path temporary = null;
        try {
            Files.createDirectories(root);
            temporary = Files.createTempFile(root, "upload-", ".part");
            long size = 0;
            try (OutputStream output = Files.newOutputStream(temporary, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (count > maximumBytes - size) {
                        throw new EvidenceSizeExceededException();
                    }
                    output.write(buffer, 0, count);
                    size += count;
                }
            }
            if (size == 0) {
                throw new BadRequestException("Evidence file must not be empty");
            }
            EvidenceImageValidator.validate(temporary, declaredType);
            String key = UUID.randomUUID().toString();
            Files.move(temporary, pathFor(key));
            temporary = null;
            return new StoredEvidence(key, declaredType, size);
        } catch (IOException ex) {
            throw new EvidenceStorageException("Could not store evidence", ex);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ex) {
                    log.error("Failed to remove temporary evidence file {}", temporary, ex);
                }
            }
        }
    }

    @Override
    public Resource load(String key) {
        Path path = pathFor(key);
        if (Files.notExists(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new ResourceNotFoundException("Evidence content not found");
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(path)) {
            throw new EvidenceStorageException("Evidence content is not a readable regular file");
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(pathFor(key));
        } catch (IOException ex) {
            throw new EvidenceStorageException("Could not remove evidence file", ex);
        }
    }

    private Path pathFor(String key) {
        try {
            if (key == null || !UUID.fromString(key).toString().equals(key)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new EvidenceStorageException("Invalid evidence storage key", ex);
        }
        return root.resolve(key);
    }
}
