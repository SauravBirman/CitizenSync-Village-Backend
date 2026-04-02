package com.citizensync.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {

    StoredMedia storeEncryptedMedia(MultipartFile mediaFile, Long issueId);

    byte[] readDecryptedMedia(String storagePath, boolean encrypted);

    record StoredMedia(
            String fileName,
            String contentType,
            long sizeBytes,
            String storagePath,
            boolean encrypted
    ) {}
}
