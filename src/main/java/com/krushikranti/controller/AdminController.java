package com.krushikranti.controller;

import com.krushikranti.dto.request.CreateDeliveryBoyRequest;
import com.krushikranti.dto.request.UpdateDeliveryBoyRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.DeliveryBoyResponse;
import com.krushikranti.dto.response.OrderResponse;
import com.krushikranti.model.Order;
import com.krushikranti.model.User;
import com.krushikranti.repository.OrderRepository;
import com.krushikranti.repository.ProductRepository;
import com.krushikranti.repository.UserRepository;
import com.krushikranti.service.DeliveryBoyService;
import com.krushikranti.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Endpoints for Admin Dashboard APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final DeliveryBoyService deliveryBoyService;
    private final OrderService orderService;

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get admin dashboard statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        List<User> allUsers = userRepository.findAll();

        long totalUsers = allUsers.size();
        long totalFarmers = allUsers.stream().filter(u -> u.getRole() == User.Role.ROLE_FARMER).count();
        long totalWholesalers = allUsers.stream().filter(u -> u.getRole() == User.Role.ROLE_WHOLESALER).count();
        long totalDeliveryBoys = allUsers.stream().filter(u -> u.getRole() == User.Role.ROLE_DELIVERY).count();
        long activeDeliveryBoys = allUsers.stream()
                .filter(u -> u.getRole() == User.Role.ROLE_DELIVERY && u.isEnabled()).count();
        long totalOrders = orderRepository.count();
        long totalProducts = productRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalFarmers", totalFarmers);
        stats.put("totalWholesalers", totalWholesalers);
        stats.put("totalDeliveryBoys", totalDeliveryBoys);
        stats.put("activeDeliveryBoys", activeDeliveryBoys);
        stats.put("totalOrders", totalOrders);
        stats.put("totalProducts", totalProducts);

        return ResponseEntity.ok(ApiResponse.success("Dashboard stats fetched successfully", stats));
    }

    // ==================== Delivery Boy Management ====================

    @PostMapping("/delivery-boys")
    @Operation(summary = "Create a new delivery boy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryBoyResponse>> createDeliveryBoy(
            @Valid @RequestBody CreateDeliveryBoyRequest request) {
        DeliveryBoyResponse response = deliveryBoyService.createDeliveryBoy(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Delivery boy created successfully", response));
    }

    @GetMapping("/delivery-boys")
    @Operation(summary = "Get all delivery boys with pagination")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<DeliveryBoyResponse>>> getAllDeliveryBoys(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean enabled) {
        Page<DeliveryBoyResponse> deliveryBoys = deliveryBoyService.getAllDeliveryBoys(page, size, enabled);
        return ResponseEntity.ok(ApiResponse.success("Delivery boys fetched successfully", deliveryBoys));
    }

    @GetMapping("/delivery-boys/active")
    @Operation(summary = "Get all active delivery boys (for dropdown selection)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DeliveryBoyResponse>>> getActiveDeliveryBoys() {
        List<DeliveryBoyResponse> deliveryBoys = deliveryBoyService.getActiveDeliveryBoys();
        return ResponseEntity.ok(ApiResponse.success("Active delivery boys fetched successfully", deliveryBoys));
    }

    @GetMapping("/delivery-boys/{id}")
    @Operation(summary = "Get delivery boy by ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryBoyResponse>> getDeliveryBoyById(@PathVariable Long id) {
        DeliveryBoyResponse response = deliveryBoyService.getDeliveryBoyById(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery boy fetched successfully", response));
    }

    @PutMapping("/delivery-boys/{id}")
    @Operation(summary = "Update delivery boy details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryBoyResponse>> updateDeliveryBoy(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeliveryBoyRequest request) {
        DeliveryBoyResponse response = deliveryBoyService.updateDeliveryBoy(id, request);
        return ResponseEntity.ok(ApiResponse.success("Delivery boy updated successfully", response));
    }

    @PutMapping("/delivery-boys/{id}/status")
    @Operation(summary = "Activate or deactivate a delivery boy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryBoyResponse>> setDeliveryBoyStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        DeliveryBoyResponse response = deliveryBoyService.setDeliveryBoyStatus(id, enabled);
        String action = enabled ? "activated" : "deactivated";
        return ResponseEntity.ok(ApiResponse.success("Delivery boy " + action + " successfully", response));
    }

    @DeleteMapping("/delivery-boys/{id}")
    @Operation(summary = "Delete a delivery boy (soft delete)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDeliveryBoy(@PathVariable Long id) {
        deliveryBoyService.deleteDeliveryBoy(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery boy deleted successfully", null));
    }

    @GetMapping("/delivery-boys/{id}/stats")
    @Operation(summary = "Get delivery statistics for a delivery boy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryBoyService.DeliveryBoyStats>> getDeliveryBoyStats(@PathVariable Long id) {
        DeliveryBoyService.DeliveryBoyStats stats = deliveryBoyService.getDeliveryBoyStats(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery boy stats fetched successfully", stats));
    }

    // ==================== Order Assignment ====================

    @GetMapping("/orders/unassigned")
    @Operation(summary = "Get orders that are not assigned to any delivery boy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUnassignedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderResponse> orders = orderService.getUnassignedOrders(page, size);
        return ResponseEntity.ok(ApiResponse.success("Unassigned orders fetched successfully", orders));
    }

    @PostMapping("/orders/{orderId}/assign")
    @Operation(summary = "Assign a delivery boy to an order")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> assignDeliveryBoy(
            @PathVariable Long orderId,
            @RequestParam Long deliveryBoyId,
            @RequestParam(required = false) String notes) {
        OrderResponse response = orderService.assignDeliveryPartner(orderId, deliveryBoyId, notes);
        return ResponseEntity.ok(ApiResponse.success("Delivery boy assigned successfully", response));
    }

    @PutMapping("/orders/{orderId}/delivery-status")
    @Operation(summary = "Update delivery status of an order (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateDeliveryStatus(
            @PathVariable Long orderId,
            @RequestParam Order.DeliveryStatus status) {
        OrderResponse response = orderService.updateDeliveryStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.success("Delivery status updated successfully", response));
    }
}
