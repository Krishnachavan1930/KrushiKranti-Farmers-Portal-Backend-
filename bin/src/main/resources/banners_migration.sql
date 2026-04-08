-- Create banners table for banner carousel
CREATE TABLE IF NOT EXISTS banners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255) NOT NULL,
    image_url LONGTEXT NOT NULL,
    redirect_url VARCHAR(255) NOT NULL,
    button_text VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    display_order INT NOT NULL DEFAULT 0,
    alt_text VARCHAR(255),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_is_active (is_active),
    INDEX idx_display_order (display_order),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample banners (optional)
INSERT INTO banners (title, subtitle, image_url, redirect_url, button_text, is_active, display_order) VALUES
('Fresh Vegetables Direct from Farmers', 'Up to 20% OFF', 'https://images.unsplash.com/photo-1488615689569-7522a3b5f57b?w=1200&h=500&fit=crop', '/products', 'Shop Now', true, 1),
('Support Local Farmers', 'Best Quality Guaranteed', 'https://images.unsplash.com/photo-1574943320219-553eb213f72d?w=1200&h=500&fit=crop', '/products', 'Explore', true, 2),
('Organic & Sustainable Farming', '100% Chemical Free Products', 'https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=1200&h=500&fit=crop', '/products', 'Discover More', true, 3);
