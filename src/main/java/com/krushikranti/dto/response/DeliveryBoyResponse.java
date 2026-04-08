package com.krushikranti.dto.response;

import com.krushikranti.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryBoyResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String profileImageUrl;
    private boolean enabled;
    private boolean verified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Delivery statistics
    private long totalAssignedOrders;
    private long pendingDeliveries;
    private long completedDeliveries;

    public static DeliveryBoyResponse fromEntity(User user) {
        return DeliveryBoyResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImageUrl(user.getProfileImageUrl())
                .enabled(user.isEnabled())
                .verified(user.isVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static DeliveryBoyResponse fromEntityWithStats(User user, long totalAssigned, long pending, long completed) {
        DeliveryBoyResponse response = fromEntity(user);
        response.setTotalAssignedOrders(totalAssigned);
        response.setPendingDeliveries(pending);
        response.setCompletedDeliveries(completed);
        return response;
    }
}
