package com.krushikranti.service;

import com.krushikranti.dto.request.CreateDeliveryBoyRequest;
import com.krushikranti.dto.request.UpdateDeliveryBoyRequest;
import com.krushikranti.dto.response.DeliveryBoyResponse;
import com.krushikranti.exception.DuplicateResourceException;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.Order;
import com.krushikranti.model.User;
import com.krushikranti.repository.OrderRepository;
import com.krushikranti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryBoyService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    /**
     * Create a new delivery boy (Admin only)
     */
    @Transactional
    public DeliveryBoyResponse createDeliveryBoy(CreateDeliveryBoyRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered: " + request.getEmail());
        }

        User deliveryBoy = User.builder()
                .name(request.getFirstName() + " " + request.getLastName())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.ROLE_DELIVERY)
                .isVerified(true) // Delivery boys created by admin don't need email verification
                .build();

        User savedUser = userRepository.save(deliveryBoy);
        log.info("New delivery boy created: {} [{}]", savedUser.getEmail(), savedUser.getId());

        return DeliveryBoyResponse.fromEntity(savedUser);
    }

    /**
     * Get all delivery boys with pagination
     */
    public Page<DeliveryBoyResponse> getAllDeliveryBoys(int page, int size, Boolean enabled) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<User> deliveryBoys;
        if (enabled != null) {
            deliveryBoys = userRepository.findByRoleAndEnabled(User.Role.ROLE_DELIVERY, enabled, pageable);
        } else {
            deliveryBoys = userRepository.findByRole(User.Role.ROLE_DELIVERY, pageable);
        }

        return deliveryBoys.map(user -> {
            long totalAssigned = orderRepository.countByDeliveryPartner(user);
            long pendingDeliveries = countPendingDeliveries(user);
            long completedDeliveries = orderRepository.countByDeliveryPartnerAndDeliveryStatus(
                    user, Order.DeliveryStatus.DELIVERED);
            return DeliveryBoyResponse.fromEntityWithStats(user, totalAssigned, pendingDeliveries, completedDeliveries);
        });
    }

    /**
     * Get all active delivery boys (for dropdown selection)
     */
    public List<DeliveryBoyResponse> getActiveDeliveryBoys() {
        List<User> activeDeliveryBoys = userRepository.findByRoleAndEnabled(User.Role.ROLE_DELIVERY, true);
        return activeDeliveryBoys.stream()
                .map(DeliveryBoyResponse::fromEntity)
                .toList();
    }

    /**
     * Get delivery boy by ID
     */
    public DeliveryBoyResponse getDeliveryBoyById(Long id) {
        User deliveryBoy = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Boy", "id", id));

        if (deliveryBoy.getRole() != User.Role.ROLE_DELIVERY) {
            throw new ResourceNotFoundException("Delivery Boy", "id", id);
        }

        long totalAssigned = orderRepository.countByDeliveryPartner(deliveryBoy);
        long pendingDeliveries = countPendingDeliveries(deliveryBoy);
        long completedDeliveries = orderRepository.countByDeliveryPartnerAndDeliveryStatus(
                deliveryBoy, Order.DeliveryStatus.DELIVERED);

        return DeliveryBoyResponse.fromEntityWithStats(deliveryBoy, totalAssigned, pendingDeliveries, completedDeliveries);
    }

    /**
     * Update delivery boy details
     */
    @Transactional
    public DeliveryBoyResponse updateDeliveryBoy(Long id, UpdateDeliveryBoyRequest request) {
        User deliveryBoy = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Boy", "id", id));

        if (deliveryBoy.getRole() != User.Role.ROLE_DELIVERY) {
            throw new ResourceNotFoundException("Delivery Boy", "id", id);
        }

        // Check if email is being changed and if it's already taken
        if (request.getEmail() != null && !request.getEmail().equals(deliveryBoy.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Email is already registered: " + request.getEmail());
            }
            deliveryBoy.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) {
            deliveryBoy.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            deliveryBoy.setLastName(request.getLastName());
        }
        if (request.getFirstName() != null || request.getLastName() != null) {
            deliveryBoy.setName(deliveryBoy.getFirstName() + " " + deliveryBoy.getLastName());
        }
        if (request.getPhone() != null) {
            deliveryBoy.setPhone(request.getPhone());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            deliveryBoy.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getEnabled() != null) {
            deliveryBoy.setEnabled(request.getEnabled());
        }

        User savedUser = userRepository.save(deliveryBoy);
        log.info("Delivery boy updated: {} [{}]", savedUser.getEmail(), savedUser.getId());

        return DeliveryBoyResponse.fromEntity(savedUser);
    }

    /**
     * Activate or deactivate a delivery boy
     */
    @Transactional
    public DeliveryBoyResponse setDeliveryBoyStatus(Long id, boolean enabled) {
        User deliveryBoy = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Boy", "id", id));

        if (deliveryBoy.getRole() != User.Role.ROLE_DELIVERY) {
            throw new ResourceNotFoundException("Delivery Boy", "id", id);
        }

        deliveryBoy.setEnabled(enabled);
        User savedUser = userRepository.save(deliveryBoy);

        String status = enabled ? "activated" : "deactivated";
        log.info("Delivery boy {}: {} [{}]", status, savedUser.getEmail(), savedUser.getId());

        return DeliveryBoyResponse.fromEntity(savedUser);
    }

    /**
     * Delete a delivery boy (soft delete by disabling)
     */
    @Transactional
    public void deleteDeliveryBoy(Long id) {
        User deliveryBoy = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Boy", "id", id));

        if (deliveryBoy.getRole() != User.Role.ROLE_DELIVERY) {
            throw new ResourceNotFoundException("Delivery Boy", "id", id);
        }

        // Check if delivery boy has pending deliveries
        long pendingDeliveries = countPendingDeliveries(deliveryBoy);
        if (pendingDeliveries > 0) {
            throw new IllegalStateException("Cannot delete delivery boy with " + pendingDeliveries + " pending deliveries. Reassign orders first.");
        }

        // Soft delete by disabling
        deliveryBoy.setEnabled(false);
        userRepository.save(deliveryBoy);

        log.info("Delivery boy soft-deleted: {} [{}]", deliveryBoy.getEmail(), deliveryBoy.getId());
    }

    /**
     * Get delivery statistics for a delivery boy
     */
    public DeliveryBoyStats getDeliveryBoyStats(Long id) {
        User deliveryBoy = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Boy", "id", id));

        if (deliveryBoy.getRole() != User.Role.ROLE_DELIVERY) {
            throw new ResourceNotFoundException("Delivery Boy", "id", id);
        }

        long totalAssigned = orderRepository.countByDeliveryPartner(deliveryBoy);
        long pendingDeliveries = countPendingDeliveries(deliveryBoy);
        long completedDeliveries = orderRepository.countByDeliveryPartnerAndDeliveryStatus(
                deliveryBoy, Order.DeliveryStatus.DELIVERED);
        long inTransit = orderRepository.countByDeliveryPartnerAndDeliveryStatus(
                deliveryBoy, Order.DeliveryStatus.IN_TRANSIT);

        return DeliveryBoyStats.builder()
                .deliveryBoyId(id)
                .totalAssignedOrders(totalAssigned)
                .pendingDeliveries(pendingDeliveries)
                .inTransitDeliveries(inTransit)
                .completedDeliveries(completedDeliveries)
                .build();
    }

    /**
     * Count pending deliveries for a delivery partner
     */
    private long countPendingDeliveries(User deliveryPartner) {
        List<Order.DeliveryStatus> pendingStatuses = List.of(
                Order.DeliveryStatus.PENDING,
                Order.DeliveryStatus.PICKUP_SCHEDULED,
                Order.DeliveryStatus.PICKED_UP,
                Order.DeliveryStatus.IN_TRANSIT,
                Order.DeliveryStatus.OUT_FOR_DELIVERY
        );
        return orderRepository.findByDeliveryPartnerAndDeliveryStatusIn(deliveryPartner, pendingStatuses).size();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class DeliveryBoyStats {
        private Long deliveryBoyId;
        private long totalAssignedOrders;
        private long pendingDeliveries;
        private long inTransitDeliveries;
        private long completedDeliveries;
    }
}
