package com.krushikranti.repository;

import com.krushikranti.model.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    /**
     * Find all active banners sorted by display order
     */
    @Query("SELECT b FROM Banner b WHERE b.isActive = true ORDER BY b.displayOrder ASC, b.createdAt DESC")
    List<Banner> findAllActiveOrderByDisplayOrder();

    /**
     * Find all banners (including inactive) sorted by display order
     */
    @Query("SELECT b FROM Banner b ORDER BY b.displayOrder ASC, b.createdAt DESC")
    List<Banner> findAllOrderByDisplayOrder();

    /**
     * Check if a banner with given title exists
     */
    boolean existsByTitleIgnoreCase(String title);

    /**
     * Find banners by active status
     */
    List<Banner> findByIsActive(Boolean isActive);
}
