package com.krushikranti.security;

import com.krushikranti.model.User;
import com.krushikranti.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.oauth2.frontend-success-url:http://localhost:5173}")
    private String frontendSuccessUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.Authentication authentication)
            throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = String.valueOf(oauth2User.getAttribute("email")).trim().toLowerCase();
        if (email.isBlank() || "null".equals(email)) {
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendSuccessUrl)
                    .queryParam("oauth2", "error")
                    .queryParam("message", "Google account email not available")
                    .build()
                    .toUriString();
            response.sendRedirect(redirectUrl);
            return;
        }

        String firstName = attributeOrDefault(oauth2User, "given_name", "Google");
        String lastName = attributeOrDefault(oauth2User, "family_name", "User");
        String fullName = (firstName + " " + lastName).trim().replaceAll("\\s+", " ");

        User user = userRepository.findByEmail(email)
                .map(existing -> {
                    existing.setEmail(email);
                    existing.setFirstName(firstName);
                    existing.setLastName(lastName);
                    existing.setName(fullName);
                    existing.setVerified(true);
                    if (existing.getRole() == null) {
                        existing.setRole(User.Role.ROLE_USER);
                    }
                    return existing;
                })
                .orElseGet(() -> User.builder()
                        .name(fullName)
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(email)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .role(User.Role.ROLE_USER)
                        .isVerified(true)
                        .build());

        User savedUser = userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(savedUser.getEmail())
                .password(savedUser.getPassword())
                .roles(savedUser.getRole().name().replace("ROLE_", ""))
                .accountExpired(!savedUser.isAccountNonExpired())
                .accountLocked(!savedUser.isAccountNonLocked())
                .credentialsExpired(!savedUser.isCredentialsNonExpired())
                .disabled(!savedUser.isEnabled())
                .build();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendSuccessUrl)
                .queryParam("oauth2", "success")
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("userId", savedUser.getId())
                .queryParam("email", savedUser.getEmail())
                .queryParam("firstName", savedUser.getFirstName())
                .queryParam("lastName", savedUser.getLastName())
                .queryParam("role", savedUser.getRole().name())
                .build()
                .toUriString();

        log.info("OAuth2 login successful for user: {}", savedUser.getEmail());
        response.sendRedirect(redirectUrl);
    }

    private String attributeOrDefault(OAuth2User user, String key, String fallback) {
        Object value = user.getAttribute(key);
        if (value == null) {
            return fallback;
        }
        String str = String.valueOf(value).trim();
        return str.isEmpty() ? fallback : str;
    }
}
