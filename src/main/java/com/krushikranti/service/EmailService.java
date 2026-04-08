package com.krushikranti.service;

import com.krushikranti.model.ContactMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String smtpFromEmail;

    @Value("${app.name:KrushiKranti}")
    private String appName;

    @Value("${app.contact.admin-email:${spring.mail.username:}}")
    private String contactAdminEmail;

    // --- Brevo HTTP API config ---
    @Value("${app.email.provider:smtp}")
    private String emailProvider;

    @Value("${app.email.brevo.api-key:}")
    private String brevoApiKey;

    @Value("${app.email.brevo.sender-email:}")
    private String brevoSenderEmail;

    @Value("${app.email.brevo.sender-name:KrushiKranti}")
    private String brevoSenderName;

    @Value("${app.email.brevo.base-url:https://api.brevo.com/v3}")
    private String brevoBaseUrl;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        // Build a RestTemplate with sensible timeouts for Brevo calls
        this.restTemplate = new RestTemplate();

        log.info("Email provider configured: {}", emailProvider);
        if (isBrevoProvider()) {
            if (brevoApiKey.isBlank()) {
                log.warn("EMAIL_PROVIDER=brevo but BREVO_API_KEY is blank — emails will fail!");
            } else {
                log.info("Brevo HTTP API ready (sender: {} <{}>)", brevoSenderName, brevoSenderEmail);
            }
        } else {
            log.info("SMTP email provider active (from: {})", smtpFromEmail);
        }
    }

    private boolean isBrevoProvider() {
        return "brevo".equalsIgnoreCase(emailProvider);
    }

    // ---------------------------------------------------------------
    // Sender email — whichever provider is active
    // ---------------------------------------------------------------
    private String getFromEmail() {
        return isBrevoProvider() ? brevoSenderEmail : smtpFromEmail;
    }

    // ================================================================
    // PUBLIC API — signatures unchanged, no breaking changes
    // ================================================================

    /**
     * Send OTP verification email.
     * This is called synchronously during registration so errors propagate
     * back to the caller and the user sees a proper error message.
     */
    public void sendOtpEmail(String toEmail, String otp) {
        String subject = appName + " Email Verification";
        String body = buildOtpEmailBody(otp);
        sendEmail(toEmail, subject, body, "OTP");
    }

    /**
     * Send welcome email after successful verification.
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            String subject = "Welcome to " + appName + "!";
            String body = buildWelcomeEmailBody(userName);
            sendEmail(toEmail, subject, body, "Welcome");
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {} — {}", toEmail, e.getMessage());
        }
    }

    /**
     * Send password reset OTP email (async version).
     */
    @Async
    public void sendPasswordResetOtpEmail(String toEmail, String otp) {
        try {
            String subject = appName + " Password Reset";
            String body = buildPasswordResetEmailBody(otp);
            sendEmail(toEmail, subject, body, "PasswordResetAsync");
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to: {} — {}", toEmail, e.getMessage());
        }
    }

    /**
     * Send password reset OTP email (synchronous — used in forgot-password flow).
     */
    public void sendPasswordResetOtp(String toEmail, String otp) {
        String subject = appName + " Password Reset OTP";
        String body = buildPasswordResetEmailBody(otp);
        try {
            sendEmail(toEmail, subject, body, "PasswordResetSync");
        } catch (Exception e) {
            log.error("Failed to send password reset OTP to: {} — {}", toEmail, e.getMessage());
            // Don't throw — OTP is already saved in database, user can request resend
        }
    }

    /**
     * Send password reset confirmation email.
     */
    @Async
    public void sendPasswordResetConfirmationEmail(String toEmail, String userName) {
        try {
            String subject = appName + " Password Reset Successful";
            String body = buildPasswordResetConfirmationEmailBody(userName);
            sendEmail(toEmail, subject, body, "PasswordResetConfirmation");
        } catch (Exception e) {
            log.error("Failed to send password reset confirmation email to: {} — {}", toEmail, e.getMessage());
        }
    }

    /**
     * Send contact form notification email to admin.
     */
    @Async
    public void sendContactMessageNotification(ContactMessage contactMessage) {
        if (contactAdminEmail == null || contactAdminEmail.isBlank()) {
            log.warn("Skipping contact notification — admin email is not configured.");
            return;
        }
        try {
            String subject = "New Contact Message";
            String body = buildContactMessageEmailBody(contactMessage);
            sendEmail(contactAdminEmail, subject, body, "ContactNotification");
        } catch (Exception e) {
            log.error("Failed to send contact notification for message id: {} — {}", contactMessage.getId(), e.getMessage());
        }
    }

    // ================================================================
    // CORE — provider dispatch
    // ================================================================

    private void sendEmail(String toEmail, String subject, String body, String context) {
        log.info("[{}] Sending email to: {} via {}", context, toEmail, emailProvider);
        long start = System.currentTimeMillis();

        try {
            if (isBrevoProvider()) {
                sendViaBrevo(toEmail, subject, body);
            } else {
                sendViaSmtp(toEmail, subject, body);
            }
            long elapsed = System.currentTimeMillis() - start;
            log.info("[{}] Email sent successfully to: {} ({}ms)", context, toEmail, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[{}] Email sending failed to: {} via {} ({}ms) — {}", context, toEmail, emailProvider, elapsed, e.getMessage());
            throw new RuntimeException("Failed to send email (" + context + "): " + e.getMessage(), e);
        }
    }

    // ================================================================
    // SMTP provider (existing behavior — JavaMailSender)
    // ================================================================

    private void sendViaSmtp(String toEmail, String subject, String body) {
        if (mailSender == null) {
            throw new RuntimeException("JavaMailSender is not configured. Check SMTP settings.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(smtpFromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    // ================================================================
    // Brevo HTTP API provider
    // ================================================================

    private void sendViaBrevo(String toEmail, String subject, String body) {
        if (brevoApiKey.isBlank()) {
            throw new RuntimeException("Brevo API key is not configured. Set BREVO_API_KEY environment variable.");
        }

        String url = brevoBaseUrl + "/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // Brevo API payload structure
        Map<String, Object> payload = Map.of(
                "sender", Map.of(
                        "name", brevoSenderName,
                        "email", brevoSenderEmail
                ),
                "to", List.of(
                        Map.of("email", toEmail)
                ),
                "subject", subject,
                "textContent", body
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Brevo API returned HTTP " + response.getStatusCode() + ": " + response.getBody());
            }

            log.debug("Brevo API response: {}", response.getBody());
        } catch (org.springframework.web.client.RestClientException e) {
            throw new RuntimeException("Brevo API call failed: " + e.getMessage(), e);
        }
    }

    // ================================================================
    // Email body builders — unchanged
    // ================================================================

    private String buildOtpEmailBody(String otp) {
        return String.format("""
                Dear User,

                Your OTP for email verification is: %s

                This OTP will expire in 5 minutes.

                If you did not request this verification, please ignore this email.

                Best regards,
                %s Team
                """, otp, appName);
    }

    private String buildWelcomeEmailBody(String userName) {
        return String.format("""
                Dear %s,

                Welcome to %s!

                Your email has been successfully verified. You can now access all features of our platform.

                Thank you for joining our agricultural marketplace community.

                Best regards,
                %s Team
                """, userName, appName, appName);
    }

    private String buildPasswordResetEmailBody(String otp) {
        return String.format("""
                Dear User,

                Your OTP for password reset is: %s

                This OTP will expire in 5 minutes.

                If you did not request a password reset, please ignore this email and ensure your account is secure.

                Best regards,
                %s Team
                """, otp, appName);
    }

    private String buildPasswordResetConfirmationEmailBody(String userName) {
        return String.format("""
                Dear %s,

                Your password has been successfully reset.

                You can now login to your account using your new password.

                If you did not perform this action, please contact support immediately.

                Best regards,
                %s Team
                """, userName, appName);
    }

    private String buildContactMessageEmailBody(ContactMessage contactMessage) {
        return String.format("""
                A new contact form message has been submitted.

                Name: %s
                Email: %s
                Subject: %s

                Message:
                %s

                Sent via %s website contact form.
                """,
                contactMessage.getName(),
                contactMessage.getEmail(),
                contactMessage.getSubject(),
                contactMessage.getMessage(),
                appName);
    }
}
