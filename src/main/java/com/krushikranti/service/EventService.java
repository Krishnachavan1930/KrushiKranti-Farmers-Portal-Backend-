package com.krushikranti.service;

import com.krushikranti.dto.request.CreateEventRequest;
import com.krushikranti.dto.request.UpdateEventRequest;
import com.krushikranti.dto.response.EventResponse;
import com.krushikranti.exception.BadRequestException;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.Event;
import com.krushikranti.model.User;
import com.krushikranti.repository.EventRepository;
import com.krushikranti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventService {

    private static final int MAX_IMAGE_PAYLOAD_LENGTH = 8_000_000;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    /**
     * Create a new event (Admin only)
     */
    public EventResponse createEvent(CreateEventRequest request, String adminEmail) {
        validateCreateRequest(request);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        if (admin == null || admin.getId() == null) {
            throw new BadRequestException("Valid creator user is required");
        }

        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .location(request.getLocation())
                .state(request.getState())
                .district(request.getDistrict())
                .zone(request.getZone())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdBy(admin)
                .build();

        try {
            Event savedEvent = eventRepository.save(event);
            log.info("Event created: {} by {}", savedEvent.getId(), adminEmail);
            return EventResponse.fromEntity(savedEvent);
        } catch (DataIntegrityViolationException ex) {
            log.error("DB constraint violation creating event", ex);
            String rootCause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            throw new BadRequestException("Failed to save event due to database constraint: " + rootCause, ex);
        } catch (Exception ex) {
            log.error("Unexpected error creating event", ex);
            throw new RuntimeException("Failed to create event: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get all events with pagination (Farmer only)
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> getAllEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findAllByOrderByStartDateAsc(pageable);
        return events.map(EventResponse::fromEntity);
    }

    /**
     * Get events with filtering (Farmer only)
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> getEventsByFilters(
            String search,
            String state,
            String district,
            String zone,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        String searchTerm = search != null && !search.isEmpty() ? search : "";
        Page<Event> events = eventRepository.findEventsByFilters(
                searchTerm,
                state,
                district,
                zone,
                fromDate,
                toDate,
                pageable);

        return events.map(EventResponse::fromEntity);
    }

    /**
     * Get upcoming events (Farmer only)
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> getUpcomingEvents(Pageable pageable) {
        LocalDate today = LocalDate.now();
        Page<Event> events = eventRepository.findByStartDateGreaterThanEqualOrderByStartDateAsc(today, pageable);
        return events.map(EventResponse::fromEntity);
    }

    /**
     * Get event by ID (Farmer can view)
     */
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        return EventResponse.fromEntity(event);
    }

    /**
     * Update event (Admin only)
     */
    public EventResponse updateEvent(Long eventId, UpdateEventRequest request, String adminEmail) {
        validateUpdateRequest(request);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setImageUrl(request.getImageUrl());
        event.setLocation(request.getLocation());
        event.setState(request.getState());
        event.setDistrict(request.getDistrict());
        event.setZone(request.getZone());
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());

        try {
            Event updatedEvent = eventRepository.save(event);
            log.info("Event updated: {} by {}", eventId, adminEmail);
            return EventResponse.fromEntity(updatedEvent);
        } catch (DataIntegrityViolationException ex) {
            log.error("DB constraint violation updating event {}", eventId, ex);
            String rootCause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            throw new BadRequestException("Failed to update event due to database constraint: " + rootCause, ex);
        } catch (Exception ex) {
            log.error("Unexpected error updating event {}", eventId, ex);
            throw new RuntimeException("Failed to update event: " + ex.getMessage(), ex);
        }
    }

    /**
     * Delete event (Admin only)
     */
    public void deleteEvent(Long eventId, String adminEmail) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        eventRepository.delete(event);
        log.info("Event deleted: {} by {}", eventId, adminEmail);
    }

    private void validateCreateRequest(CreateEventRequest request) {
        if (request == null) {
            throw new BadRequestException("Event request body is required");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BadRequestException("Event title is required");
        }
        if (request.getStartDate() == null) {
            throw new BadRequestException("Start date is required");
        }
        if (request.getEndDate() == null) {
            throw new BadRequestException("End date is required");
        }
        validateImagePayloadSize(request.getImageUrl());
    }

    private void validateUpdateRequest(UpdateEventRequest request) {
        if (request == null) {
            throw new BadRequestException("Event request body is required");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BadRequestException("Event title is required");
        }
        if (request.getStartDate() == null) {
            throw new BadRequestException("Start date is required");
        }
        if (request.getEndDate() == null) {
            throw new BadRequestException("End date is required");
        }
        validateImagePayloadSize(request.getImageUrl());
    }

    private void validateImagePayloadSize(String imageUrl) {
        if (imageUrl != null && imageUrl.length() > MAX_IMAGE_PAYLOAD_LENGTH) {
            throw new BadRequestException("Image payload too large. Please upload a smaller image.");
        }
    }
}
