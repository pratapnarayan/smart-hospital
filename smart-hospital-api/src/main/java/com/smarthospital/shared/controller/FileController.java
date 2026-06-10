package com.smarthospital.shared.controller;

import com.smarthospital.shared.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private static final Map<String, String> EXT_TYPE = Map.of(
        ".jpg",  "image/jpeg",
        ".jpeg", "image/jpeg",
        ".png",  "image/png",
        ".webp", "image/webp"
    );

    private final FileStorageService storage;

    public FileController(FileStorageService storage) { this.storage = storage; }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource resource = storage.load(filename);
        String contentType = EXT_TYPE.getOrDefault(
            filename.substring(filename.lastIndexOf('.')).toLowerCase(), "image/jpeg");
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource);
    }
}
