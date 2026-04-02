package com.citizensync.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.media.storage-provider", havingValue = "local", matchIfMissing = true)
public class LocalEncryptedMediaStorageService implements MediaStorageService {

    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    @Value("${app.media.upload-dir:uploads/issues}")
    private String uploadDir;

    @Value("${app.media.max-size-bytes:52428800}")
    private long maxSizeBytes;

    @Value("${app.media.encryption-secret:change-me-in-production-citizensync}")
    private String encryptionSecret;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public StoredMedia storeEncryptedMedia(MultipartFile mediaFile, Long issueId) {
        validateMedia(mediaFile);

        try {
            byte[] plaintext = mediaFile.getBytes();
            byte[] iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);

            String safeName = sanitizeFileName(mediaFile.getOriginalFilename());
            String storedName = "issue-" + issueId + "-" + UUID.randomUUID() + ".enc";
            Path storedPath = directory.resolve(storedName);

            // File format: [12-byte IV][ciphertext+tag]
            Files.write(storedPath, iv, StandardOpenOption.CREATE_NEW);
            Files.write(storedPath, ciphertext, StandardOpenOption.APPEND);

            return new StoredMedia(
                    safeName,
                    mediaFile.getContentType(),
                    mediaFile.getSize(),
                    storedPath.toString(),
                    true
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to encrypt and store media", ex);
        }
    }

    @Override
    public byte[] readDecryptedMedia(String storagePath, boolean encrypted) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new RuntimeException("Media file path is missing");
        }

        try {
            Path path = Paths.get(storagePath).toAbsolutePath().normalize();
            byte[] raw = Files.readAllBytes(path);

            if (!encrypted) {
                return raw;
            }

            if (raw.length <= GCM_IV_BYTES) {
                throw new RuntimeException("Encrypted media payload is invalid");
            }

            byte[] iv = new byte[GCM_IV_BYTES];
            System.arraycopy(raw, 0, iv, 0, GCM_IV_BYTES);

            byte[] ciphertext = new byte[raw.length - GCM_IV_BYTES];
            System.arraycopy(raw, GCM_IV_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read/decrypt media", ex);
        }
    }

    private void validateMedia(MultipartFile mediaFile) {
        if (mediaFile == null || mediaFile.isEmpty()) {
            throw new RuntimeException("Media file is empty");
        }

        if (mediaFile.getSize() > maxSizeBytes) {
            throw new RuntimeException("Media file exceeds max allowed size of " + maxSizeBytes + " bytes");
        }

        String contentType = mediaFile.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            throw new RuntimeException("Only image and video files are allowed");
        }
    }

    private SecretKeySpec buildKey() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(encryptionSecret.getBytes());
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to derive encryption key", ex);
        }
    }

    private String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "upload";
        }
        String normalized = Normalizer.normalize(originalName, Normalizer.Form.NFKC)
                .replace("\\", "_")
                .replace("/", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        return normalized.length() > 140 ? normalized.substring(normalized.length() - 140) : normalized;
    }
}
