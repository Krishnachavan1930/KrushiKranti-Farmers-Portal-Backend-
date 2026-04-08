package com.krushikranti.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 190, message = "Email must be at most 190 characters")
    private String email;

    @NotBlank(message = "Subject is required")
    @Size(min = 2, max = 180, message = "Subject must be between 2 and 180 characters")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(min = 5, max = 5000, message = "Message must be between 5 and 5000 characters")
    private String message;
}
