package com.smarthospital.shared.service;

import com.smarthospital.core.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    @Value("${app.upload.dir:./uploads/photos}")
    private String uploadDir;

    private Path storagePath;

    @PostConstruct
    public void init() {
        storagePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storagePath);
            log.info("Photo storage directory: {}", storagePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + storagePath, e);
        }
    }

    public String store(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty())
            throw ApiException.badRequest("FILE_EMPTY", "Uploaded file is empty");
        if (!ALLOWED_TYPES.contains(file.getContentType()))
            throw ApiException.badRequest("FILE_TYPE", "Only JPEG, PNG and WebP images are allowed");
        if (file.getSize() > MAX_SIZE_BYTES)
            throw ApiException.badRequest("FILE_TOO_LARGE", "File must be under 5 MB");

        String ext = getExtension(file.getOriginalFilename());
        String filename = prefix + "_" + UUID.randomUUID() + ext;
        Path target = storagePath.resolve(filename);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + filename, e);
        }
        return "/api/v1/files/" + filename;
    }

    public Resource load(String filename) {
        try {
            Path file = storagePath.resolve(filename).normalize();
            if (!file.startsWith(storagePath))
                throw ApiException.badRequest("INVALID_PATH", "Invalid file path");
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable())
                throw ApiException.notFound("FILE_NOT_FOUND", "File not found: " + filename);
            return resource;
        } catch (MalformedURLException e) {
            throw ApiException.notFound("FILE_NOT_FOUND", "File not found: " + filename);
        }
    }

    public void delete(String urlPath) {
        if (urlPath == null) return;
        String filename = urlPath.substring(urlPath.lastIndexOf('/') + 1);
        Path file = storagePath.resolve(filename).normalize();
        try { Files.deleteIfExists(file); } catch (IOException ignored) {}
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : ".jpg";
    }
}
