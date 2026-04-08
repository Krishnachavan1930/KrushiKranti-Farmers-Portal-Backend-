package com.krushikranti.service;

import com.krushikranti.model.Banner;
import com.krushikranti.repository.BannerRepository;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BannerService {

    private final BannerRepository bannerRepository;
    private static final long MAX_BANNER_IMAGE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    @Value("${app.upload.banner-dir:uploads/banners}")
    private String bannerUploadDir;

    /**
     * Get all active banners
     */
    @Transactional(readOnly = true)
    public List<Banner> getActiveBanners() {
        log.debug("Fetching all active banners");
        return bannerRepository.findAllActiveOrderByDisplayOrder();
    }

    /**
     * Get all banners (including inactive)
     */
    @Transactional(readOnly = true)
    public List<Banner> getAllBanners() {
        log.debug("Fetching all banners");
        return bannerRepository.findAllOrderByDisplayOrder();
    }

    /**
     * Get a banner by ID
     */
    @Transactional(readOnly = true)
    public Optional<Banner> getBannerById(Long id) {
        log.debug("Fetching banner with ID: {}", id);
        return bannerRepository.findById(id);
    }

    /**
     * Create a new banner
     */
    public Banner createBanner(Banner banner) {
        log.info("Creating new banner with title: {}", banner.getTitle());

        if (bannerRepository.existsByTitleIgnoreCase(banner.getTitle())) {
            log.warn("Banner with title {} already exists", banner.getTitle());
            throw new IllegalArgumentException("Banner with this title already exists");
        }

        if (banner.getIsActive() == null) {
            banner.setIsActive(true);
        }
        return bannerRepository.save(banner);
    }

    /**
     * Save banner image to local uploads directory and return relative URL path.
     */
    public String saveBannerImage(MultipartFile image) {
        validateBannerImage(image);

        try {
            Path uploadPath = Path.of(bannerUploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String extension = getFileExtension(image.getOriginalFilename());
            String fileName = "banner-" + UUID.randomUUID() + extension;
            Path targetPath = uploadPath.resolve(fileName);

            Files.copy(image.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/banners/" + fileName;
        } catch (IOException e) {
            log.error("Failed to store banner image", e);
            throw new RuntimeException("Failed to upload banner image", e);
        }
    }

    private void validateBannerImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Banner image is required");
        }

        if (image.getSize() > MAX_BANNER_IMAGE_SIZE) {
            throw new IllegalArgumentException("Banner image exceeds 10MB limit");
        }

        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Invalid image type. Allowed: JPEG, PNG, WEBP, GIF");
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
    }

    /**
     * Update an existing banner
     */
    public Banner updateBanner(Long id, Banner updateData) {
        log.info("Updating banner with ID: {}", id);

        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Banner not found with ID: {}", id);
                    return new RuntimeException("Banner not found with ID: " + id);
                });

        if (updateData.getTitle() != null) {
            banner.setTitle(updateData.getTitle());
        }
        if (updateData.getSubtitle() != null) {
            banner.setSubtitle(updateData.getSubtitle());
        }
        if (updateData.getImageUrl() != null) {
            banner.setImageUrl(updateData.getImageUrl());
        }
        if (updateData.getRedirectUrl() != null) {
            banner.setRedirectUrl(updateData.getRedirectUrl());
        }
        if (updateData.getButtonText() != null) {
            banner.setButtonText(updateData.getButtonText());
        }
        if (updateData.getIsActive() != null) {
            banner.setIsActive(updateData.getIsActive());
        }
        if (updateData.getDisplayOrder() != null) {
            banner.setDisplayOrder(updateData.getDisplayOrder());
        }
        if (updateData.getAltText() != null) {
            banner.setAltText(updateData.getAltText());
        }

        return bannerRepository.save(banner);
    }

    /**
     * Delete a banner
     */
    public void deleteBanner(Long id) {
        log.info("Deleting banner with ID: {}", id);

        if (!bannerRepository.existsById(id)) {
            log.error("Banner not found with ID: {}", id);
            throw new RuntimeException("Banner not found with ID: " + id);
        }

        bannerRepository.deleteById(id);
    }

    /**
     * Toggle banner active status
     */
    public Banner toggleBannerStatus(Long id) {
        log.info("Toggling status of banner with ID: {}", id);

        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Banner not found with ID: {}", id);
                    return new RuntimeException("Banner not found with ID: " + id);
                });

        banner.setIsActive(!banner.getIsActive());
        return bannerRepository.save(banner);
    }

    /**
     * Get banners by active status
     */
    @Transactional(readOnly = true)
    public List<Banner> getBannersByStatus(Boolean isActive) {
        log.debug("Fetching banners with status: {}", isActive);
        return bannerRepository.findByIsActive(isActive);
    }
}
