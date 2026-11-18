package com.gyanganga.school.web;

import com.gyanganga.school.service.FileStorageService;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class UploadController {
    private final FileStorageService storage;

    public UploadController(FileStorageService storage) {
        this.storage = storage;
    }

    /** Uploads one public image (resized, EXIF stripped). Admin only. */
    @PostMapping("/uploads")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) {
        return Map.of("url", storage.storeImage(file));
    }

    /** Private documents are served only to signed-in staff. */
    @GetMapping("/files/{name:.+}")
    public ResponseEntity<Resource> download(@PathVariable String name) {
        Path p = storage.privateFile(name);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(FileStorageService.contentType(name)))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(name).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(new FileSystemResource(p));
    }
}
