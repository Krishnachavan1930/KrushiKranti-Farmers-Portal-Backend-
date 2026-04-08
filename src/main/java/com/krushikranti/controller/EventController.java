package com.krushikranti.controller;

import com.krushikranti.dto.request.CreateEventRequest;
import com.krushikranti.dto.request.UpdateEventRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.EventResponse;
import com.krushikranti.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Events", description = "Endpoints for managing agricultural events")
public class EventController {

    private final EventService eventService;

    // ────────────────────────────────────────────────────────────────
    // FARMER ENDPOINTS (Public Read)
    // ────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get all events (Farmer only)", description = "Returns paginated list of events")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<EventResponse> events = eventService.getAllEvents(pageable);
        return ResponseEntity.ok(ApiResponse.success("Events fetched successfully", events));
    }

    @GetMapping("/filter")
    @Operation(summary = "Get events with filters (Farmer only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getEventsByFilters(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());
        Page<EventResponse> events = eventService.getEventsByFilters(
                search, state, district, zone, fromDate, toDate, pageable);

        return ResponseEntity.ok(ApiResponse.success("Filtered events fetched successfully", events));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming events (Farmer only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getUpcomingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());
        Page<EventResponse> events = eventService.getUpcomingEvents(pageable);
        return ResponseEntity.ok(ApiResponse.success("Upcoming events fetched successfully", events));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID (Farmer only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(@PathVariable Long id) {
        EventResponse event = eventService.getEventById(id);
        return ResponseEntity.ok(ApiResponse.success("Event fetched successfully", event));
    }

    // ────────────────────────────────────────────────────────────────
    // ADMIN ENDPOINTS (CRUD)
    // ────────────────────────────────────────────────────────────────

    @PostMapping("/admin/events")
    @Operation(summary = "Create a new event (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication) {

        log.info("User role: {}", authentication.getAuthorities());
        log.info("Create event request by {} - title: {}, startDate: {}, endDate: {}",
                authentication.getName(), request.getTitle(), request.getStartDate(), request.getEndDate());

        EventResponse event = eventService.createEvent(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event created successfully", event));
    }

    @PutMapping("/admin/events/{id}")
    @Operation(summary = "Update an event (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request,
            Authentication authentication) {

        log.info("User role: {}", authentication.getAuthorities());
        log.info("Update event request by {} - eventId: {}, title: {}, startDate: {}, endDate: {}",
                authentication.getName(), id, request.getTitle(), request.getStartDate(), request.getEndDate());

        EventResponse event = eventService.updateEvent(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Event updated successfully", event));
    }

    @DeleteMapping("/admin/events/{id}")
    @Operation(summary = "Delete an event (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable Long id,
            Authentication authentication) {

        eventService.deleteEvent(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Event deleted successfully", null));
    }

    @GetMapping("/admin/events")
    @Operation(summary = "Get all events for admin (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getAdminEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        log.info("User role: {}", authentication.getAuthorities());

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<EventResponse> events = eventService.getAllEvents(pageable);
        return ResponseEntity.ok(ApiResponse.success("Admin events fetched successfully", events));
    }
}
