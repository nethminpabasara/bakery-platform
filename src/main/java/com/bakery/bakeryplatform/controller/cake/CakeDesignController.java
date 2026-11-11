package com.bakery.bakeryplatform.controller.cake;

import com.bakery.bakeryplatform.model.cake.CakeDesign;
import com.bakery.bakeryplatform.service.cake.CakeDesignService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cake-designs")
public class CakeDesignController {

    private static final long MAX_REFERENCE_IMAGE_SIZE = 5L * 1024 * 1024;
    private final CakeDesignService cakeDesignService;
    private final Path referenceImageDirectory;

    public CakeDesignController(
            CakeDesignService cakeDesignService,
            @Value("${app.cake-reference-images:data/cake-reference-images}") String imageDirectory) {
        this.cakeDesignService = cakeDesignService;
        this.referenceImageDirectory = Paths.get(imageDirectory).toAbsolutePath().normalize();
    }

    // Create: Customer saves a new custom design
    @PostMapping
    public Map<String, Object> saveDesign(@RequestBody CakeDesign design) {
        CakeDesign savedDesign = cakeDesignService.saveDesign(design);
        return Map.of("success", true, "designCode", "DES-" + savedDesign.getId());
    }

    // Read: Customer views their saved designs
    @GetMapping
    public List<CakeDesign> getSavedDesigns() {
        return cakeDesignService.getAllDesigns();
    }

    // Update: Customer edits a saved design
    @PutMapping("/{id}")
    public Map<String, Object> updateDesign(@PathVariable Long id, @RequestBody CakeDesign design) {
        cakeDesignService.updateDesign(id, design);
        return Map.of("success", true, "message", "Design updated successfully");
    }

    // Delete: Customer deletes a saved design
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteDesign(@PathVariable Long id) {
        cakeDesignService.deleteDesign(id);
        return Map.of("success", true, "message", "Design deleted successfully");
    }

    // Upload: Handle reference image uploads
    @PostMapping("/upload")
    public Map<String, Object> uploadReferenceImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a reference image to upload.");
        }
        if (file.getSize() > MAX_REFERENCE_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reference images must be 5 MB or smaller.");
        }

        byte[] imageBytes = file.getBytes();
        String extension = detectImageExtension(imageBytes);
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a valid PNG, JPEG, or WebP image.");
        }

        Files.createDirectories(referenceImageDirectory);
        String filename = UUID.randomUUID() + extension;
        Files.write(referenceImageDirectory.resolve(filename), imageBytes,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        String imageUrl = "/api/cake-designs/uploads/" + filename;
        return Map.of("success", true, "imageUrl", imageUrl);
    }

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> getReferenceImage(@PathVariable String filename) throws IOException {
        if (!filename.matches("[0-9a-fA-F-]+\\.(png|jpg|webp)")) {
            return ResponseEntity.notFound().build();
        }
        Path imagePath = referenceImageDirectory.resolve(filename).normalize();
        if (!imagePath.startsWith(referenceImageDirectory) || !Files.isRegularFile(imagePath)) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(imagePath);
        MediaType mediaType = contentType == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(mediaType)
                .body(new FileSystemResource(imagePath));
    }

    private String detectImageExtension(byte[] bytes) {
        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return ".png";
        }
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF) {
            return ".jpg";
        }
        if (bytes.length >= 12 && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return ".webp";
        }
        return null;
    }
}