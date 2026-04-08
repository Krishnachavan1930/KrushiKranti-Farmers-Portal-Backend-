package com.krushikranti.controller;

import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.DeliveryBoyResponse;
import com.krushikranti.dto.response.OrderResponse;
import com.krushikranti.model.Order;
import com.krushikranti.model.User;
import com.krushikranti.repository.UserRepository;
import com.krushikranti.service.DeliveryBoyService;
import com.krushikranti.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery", description = "Endpoints for Delivery Boy operations")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    private final OrderService orderService;
    private final DeliveryBoyService deliveryBoyService;
    private final UserRepository userRepository;

    @GetMapping("/profile")
    @Operation(summary = "Get delivery boy's own profile")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<ApiResponse<DeliveryBoyResponse>> getProfile(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        DeliveryBoyResponse response = deliveryBoyService.getDeliveryBoyById(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", response));
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get delivery boy's dashboard statistics")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DeliveryBoyService.DeliveryBoyStats stats = deliveryBoyService.getDeliveryBoyStats(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("totalAssignedOrders", stats.getTotalAssignedOrders());
        response.put("pendingDeliveries", stats.getPendingDeliveries());
        response.put("inTransitDeliveries", stats.getInTransitDeliveries());
        response.put("completedDeliveries", stats.getCompletedDeliveries());

        return ResponseEntity.ok(ApiResponse.success("Dashboard stats fetched successfully", response));
    }

    @GetMapping("/orders")
    @Operation(summary = "Get orders assigned to the delivery boy")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Order.DeliveryStatus status) {
        String email = authentication.getName();
        Page<OrderResponse> orders = orderService.getDeliveryPartnerOrders(email, page, size, status);
        return ResponseEntity.ok(ApiResponse.success("Orders fetched successfully", orders));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get order details by ID (only if assigned to this delivery boy)")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            Authentication authentication,
            @PathVariable Long orderId) {
        String email = authentication.getName();
        OrderResponse order = orderService.getOrderById(orderId);

        // Verify the order is assigned to this delivery boy
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (order.getDeliveryPartnerId() == null || !order.getDeliveryPartnerId().equals(user.getId())) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("You are not authorized to view this order"));
        }

        return ResponseEntity.ok(ApiResponse.success("Order fetched successfully", order));
    }

    @PutMapping("/orders/{orderId}/status")
    @Operation(summary = "Update delivery status of an order")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateDeliveryStatus(
            Authentication authentication,
            @PathVariable Long orderId,
            @RequestParam Order.DeliveryStatus status) {
        String email = authentication.getName();
        OrderResponse order = orderService.getOrderById(orderId);

        // Verify the order is assigned to this delivery boy
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (order.getDeliveryPartnerId() == null || !order.getDeliveryPartnerId().equals(user.getId())) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("You are not authorized to update this order"));
        }

        // Validate status transition
        if (!isValidStatusTransition(order.getDeliveryStatus(), status.name())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid status transition from " + order.getDeliveryStatus() + " to " + status));
        }

        OrderResponse response = orderService.updateDeliveryStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.success("Delivery status updated successfully", response));
    }

    @PutMapping("/orders/{orderId}/pickup")
    @Operation(summary = "Mark order as picked up")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<ApiResponse<OrderResponse>> markAsPickedUp(
            Authentication authentication,
            @PathVariable Long orderId) {
        return updateDeliveryStatus(authentication, orderId, Order.DeliveryStatus.PICKED_UP);
    }

    @PutMapping("/orders/{orderId}/in-transit")
    @Operation(summary = "Mark order as in transit")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<ApiResponse<OrderResponse>> markAsInTransit(
            Authentication authentication,
            @PathVariable Long orderId) {
        return updateDeliveryStatus(authentication, orderId, Order.DeliveryStatus.IN_TRANSIT);
    }

    @PutMapping("/orders/{orderId}/out-for-delivery")
    @Operation(summary = "Mark order as out for delivery")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<ApiResponse<OrderResponse>> markAsOutForDelivery(
            Authentication authentication,
            @PathVariable Long orderId) {
        return updateDeliveryStatus(authentication, orderId, Order.DeliveryStatus.OUT_FOR_DELIVERY);
    }

    @PutMapping("/orders/{orderId}/delivered")
    @Operation(summary = "Mark order as delivered")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<ApiResponse<OrderResponse>> markAsDelivered(
            Authentication authentication,
            @PathVariable Long orderId) {
        return updateDeliveryStatus(authentication, orderId, Order.DeliveryStatus.DELIVERED);
    }

    /**
     * Validate delivery status transitions
     */
    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus == null) currentStatus = "PENDING";

        return switch (currentStatus) {
            case "PENDING" -> newStatus.equals("PICKUP_SCHEDULED") || newStatus.equals("CANCELLED");
            case "PICKUP_SCHEDULED" -> newStatus.equals("PICKED_UP") || newStatus.equals("CANCELLED");
            case "PICKED_UP" -> newStatus.equals("IN_TRANSIT") || newStatus.equals("CANCELLED");
            case "IN_TRANSIT" -> newStatus.equals("OUT_FOR_DELIVERY") || newStatus.equals("CANCELLED");
            case "OUT_FOR_DELIVERY" -> newStatus.equals("DELIVERED") || newStatus.equals("RETURNED") || newStatus.equals("CANCELLED");
            case "DELIVERED", "CANCELLED", "RETURNED" -> false; // Terminal states
            default -> false;
        };
    }
}
