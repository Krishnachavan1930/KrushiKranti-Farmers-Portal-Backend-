package com.krushikranti.controller;

import com.krushikranti.dto.request.ContactMessageRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.ContactMessageResponse;
import com.krushikranti.service.ContactMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Contact Messages", description = "Public contact form and admin contact inbox endpoints")
public class ContactMessageController {

    private final ContactMessageService contactMessageService;

    @PostMapping("/contact")
    @Operation(summary = "Submit contact form")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> submitContactMessage(
            @Valid @RequestBody ContactMessageRequest request
    ) {
        ContactMessageResponse response = contactMessageService.submitMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Contact message submitted successfully", response));
    }

    @GetMapping("/admin/contact-messages")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all contact messages (admin only)")
    public ResponseEntity<ApiResponse<List<ContactMessageResponse>>> getAllContactMessages() {
        return ResponseEntity.ok(
                ApiResponse.success("Contact messages fetched successfully", contactMessageService.getAllMessages())
        );
    }

    @GetMapping("/admin/contact-messages/unread-count")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get unread contact message count (admin only)")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadContactMessageCount() {
        return ResponseEntity.ok(
                ApiResponse.success("Unread contact message count fetched", Map.of("count", contactMessageService.getUnreadCount()))
        );
    }

    @PutMapping("/admin/contact-messages/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mark a contact message as read (admin only)")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> markContactMessageAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Contact message marked as read", contactMessageService.markAsRead(id))
        );
    }

    @DeleteMapping("/admin/contact-messages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a contact message (admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteContactMessage(@PathVariable Long id) {
        contactMessageService.deleteMessage(id);
        return ResponseEntity.ok(ApiResponse.success("Contact message deleted successfully", null));
    }
}
