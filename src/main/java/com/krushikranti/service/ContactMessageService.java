package com.krushikranti.service;

import com.krushikranti.dto.request.ContactMessageRequest;
import com.krushikranti.dto.response.ContactMessageResponse;
import com.krushikranti.exception.BadRequestException;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.ContactMessage;
import com.krushikranti.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final EmailService emailService;

    @Transactional
    public ContactMessageResponse submitMessage(ContactMessageRequest request) {
        String name = sanitize(request.getName());
        String email = sanitize(request.getEmail());
        String subject = sanitize(request.getSubject());
        String message = sanitize(request.getMessage());

        if (name.isBlank() || email.isBlank() || subject.isBlank() || message.isBlank()) {
            throw new BadRequestException("All contact fields are required");
        }

        ContactMessage saved = contactMessageRepository.save(
                ContactMessage.builder()
                        .name(name)
                        .email(email)
                        .subject(subject)
                        .message(message)
                        .status(ContactMessage.Status.UNREAD)
                        .build()
        );

        emailService.sendContactMessageNotification(saved);
        return ContactMessageResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ContactMessageResponse> getAllMessages() {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ContactMessageResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return contactMessageRepository.countByStatus(ContactMessage.Status.UNREAD);
    }

    @Transactional
    public ContactMessageResponse markAsRead(Long id) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContactMessage", "id", id));

        if (message.getStatus() != ContactMessage.Status.READ) {
            message.setStatus(ContactMessage.Status.READ);
            message = contactMessageRepository.save(message);
        }

        return ContactMessageResponse.fromEntity(message);
    }

    @Transactional
    public void deleteMessage(Long id) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContactMessage", "id", id));
        contactMessageRepository.delete(message);
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }
}
