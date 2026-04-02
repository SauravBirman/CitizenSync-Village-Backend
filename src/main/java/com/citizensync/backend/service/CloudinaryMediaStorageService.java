package com.citizensync.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URL;
import java.text.Normalizer;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.media.storage-provider", havingValue = "cloudinary")
public class CloudinaryMediaStorageService implements MediaStorageService {

    private final Cloudinary cloudinary;

    @Value("${app.media.max-size-bytes:52428800}")
    private long maxSizeBytes;

    @Value("${app.media.cloudinary.folder:citizensync/issues}")
    private String folder;

    public CloudinaryMediaStorageService(
            @Value("${app.media.cloudinary.cloud-name}") String cloudName,
            @Value("${app.media.cloudinary.api-key}") String apiKey,
            @Value("${app.media.cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    @Override
    @SuppressWarnings("unchecked")
    public StoredMedia storeEncryptedMedia(MultipartFile mediaFile, Long issueId) {
        validateMedia(mediaFile);

        try {
            String contentType = mediaFile.getContentType();
            String resourceType = contentType != null && contentType.startsWith("video/") ? "video" : "image";

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    mediaFile.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "auto",
                            "folder", folder,
                            "public_id", "issue-" + issueId + "-" + System.currentTimeMillis(),
                            "overwrite", false
                    )
            );

            String publicId = (String) uploadResult.get("public_id");
            String safeName = sanitizeFileName(mediaFile.getOriginalFilename());

            return new StoredMedia(
                    safeName,
                    contentType,
                    mediaFile.getSize(),
                    resourceType + ":" + publicId,
                    false
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload media to Cloudinary", ex);
        }
    }

    @Override
    public byte[] readDecryptedMedia(String storagePath, boolean encrypted) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new RuntimeException("Cloudinary media key is missing");
        }

        try {
            String[] parts = storagePath.split(":", 2);
            if (parts.length != 2) {
                throw new RuntimeException("Cloudinary media key is invalid");
            }

            String resourceType = parts[0];
            String publicId = parts[1];
            String mediaUrl = cloudinary.url()
                    .resourceType(resourceType)
                    .secure(true)
                    .generate(publicId);

            try (var inputStream = new URI(mediaUrl).toURL().openStream()) {
                return inputStream.readAllBytes();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read media from Cloudinary", ex);
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
