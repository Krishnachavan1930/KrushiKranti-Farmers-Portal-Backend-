package com.krushikranti.dto.response;

import com.krushikranti.model.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String location;
    private String state;
    private String district;
    private String zone;
    private LocalDate startDate;
    private LocalDate endDate;
    private String createdByName;
    private String createdByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EventResponse fromEntity(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .imageUrl(event.getImageUrl())
                .location(event.getLocation())
                .state(event.getState())
                .district(event.getDistrict())
                .zone(event.getZone())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .createdByName(event.getCreatedBy().getFirstName() + " " + event.getCreatedBy().getLastName())
                .createdByEmail(event.getCreatedBy().getEmail())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
