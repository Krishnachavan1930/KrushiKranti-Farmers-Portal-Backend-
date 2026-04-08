package com.krushikranti.controller;

import com.krushikranti.model.Banner;
import com.krushikranti.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
@Slf4j
public class BannerController {

    private final BannerService bannerService;

    /**
     * Get all active banners (Public endpoint)
     * Used by the banner carousel on the home page
     */
    @GetMapping
    public ResponseEntity<List<Banner>> getActiveBanners() {
        log.info("GET /api/v1/banners - Fetching active banners");
        List<Banner> banners = bannerService.getActiveBanners();
        return ResponseEntity.ok(banners);
    }

    /**
     * Get only active banners (explicit endpoint for homepage integration)
     */
    @GetMapping("/active")
    public ResponseEntity<List<Banner>> getActiveBannersExplicit() {
        log.info("GET /api/v1/banners/active - Fetching active banners");
        List<Banner> banners = bannerService.getActiveBanners();
        return ResponseEntity.ok(banners);
    }

    /**
     * Get a single banner by ID (Public endpoint)
     */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Banner> getBannerById(@PathVariable Long id) {
        log.info("GET /api/v1/banners/{} - Fetching banner by ID", id);
        return bannerService.getBannerById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get all banners including inactive (Admin only)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Banner>> getAllBanners() {
        log.info("GET /api/v1/banners/admin/all - Fetching all banners (admin)");
        List<Banner> banners = bannerService.getAllBanners();
        return ResponseEntity.ok(banners);
    }

    /**
     * Create a new banner (Admin only)
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createBanner(@RequestBody Banner banner) {
        log.info("POST /api/v1/banners - Creating new banner with title: {}", banner.getTitle());
        try {
            Banner createdBanner = bannerService.createBanner(banner);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBanner);
        } catch (IllegalArgumentException e) {
            log.error("Error creating banner: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create banner with file upload (Admin only)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createBannerWithImage(
            @RequestParam("title") String title,
            @RequestParam(value = "subtitle", required = false, defaultValue = "") String subtitle,
            @RequestParam(value = "buttonText", required = false, defaultValue = "Shop Now") String buttonText,
            @RequestParam("redirectUrl") String redirectUrl,
            @RequestParam(value = "isActive", required = false, defaultValue = "true") Boolean isActive,
            @RequestParam("image") MultipartFile image) {
        log.info("POST /api/v1/banners (multipart) - Creating banner with title: {}", title);
        try {
            String relativePath = bannerService.saveBannerImage(image);
            String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(relativePath)
                    .toUriString();

            Banner banner = Banner.builder()
                    .title(title)
                    .subtitle(subtitle)
                    .buttonText(buttonText)
                    .redirectUrl(redirectUrl)
                    .imageUrl(imageUrl)
                    .isActive(isActive)
                    .build();

            Banner createdBanner = bannerService.createBanner(banner);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBanner);
        } catch (IllegalArgumentException e) {
            log.error("Error creating banner with image: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update a banner (Admin only)
     */
    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateBanner(@PathVariable Long id, @RequestBody Banner updateData) {
        log.info("PUT /api/v1/banners/{} - Updating banner", id);
        try {
            Banner updatedBanner = bannerService.updateBanner(id, updateData);
            return ResponseEntity.ok(updatedBanner);
        } catch (RuntimeException e) {
            log.error("Error updating banner: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a banner (Admin only)
     */
    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBanner(@PathVariable Long id) {
        log.info("DELETE /api/v1/banners/{} - Deleting banner", id);
        try {
            bannerService.deleteBanner(id);
            return ResponseEntity.ok(Map.of("message", "Banner deleted successfully"));
        } catch (RuntimeException e) {
            log.error("Error deleting banner: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Toggle banner active status (Admin only)
     */
    @PatchMapping("/{id:\\d+}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleBannerStatus(@PathVariable Long id) {
        log.info("PATCH /api/v1/banners/{}/toggle - Toggling banner status", id);
        try {
            Banner banner = bannerService.toggleBannerStatus(id);
            return ResponseEntity.ok(banner);
        } catch (RuntimeException e) {
            log.error("Error toggling banner status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get banners by status (Admin only)
     */
    @GetMapping("/admin/by-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Banner>> getBannersByStatus(@RequestParam Boolean isActive) {
        log.info("GET /api/v1/banners/admin/by-status - Fetching banners with status: {}", isActive);
        List<Banner> banners = bannerService.getBannersByStatus(isActive);
        return ResponseEntity.ok(banners);
    }
}
