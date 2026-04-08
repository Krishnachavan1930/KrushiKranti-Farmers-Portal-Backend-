package com.krushikranti.config;

import com.krushikranti.security.JwtAuthenticationFilter;
import com.krushikranti.security.OAuth2LoginSuccessHandler;
import com.krushikranti.security.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;

import org.springframework.beans.factory.annotation.Value;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Value("${app.oauth2.frontend-success-url:http://localhost:5173}")
    private String frontendUrl;

    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/api/v1/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/uploads/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/contact").permitAll()
                        // Banner endpoints: public read, admin write
                        .requestMatchers(HttpMethod.GET, "/api/v1/banners").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/banners/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/banners/{id}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/banners/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/banners/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/banners/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/banners/**").hasRole("ADMIN")
                        // Product endpoints — public GET access
                        .requestMatchers(HttpMethod.GET, "/api/v1/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/{id}").permitAll()
                        // Product farmer endpoints — require authentication (method-level @PreAuthorize
                        // handles role)
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/my-products").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/farmer/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").authenticated()
                        // Blog endpoints — public GET access
                        .requestMatchers(HttpMethod.GET, "/api/v1/blogs/**").permitAll()
                        // Upload endpoints (require authentication)
                        .requestMatchers("/api/v1/upload/**").authenticated()
                        // WebSocket endpoints
                        .requestMatchers("/ws/**").permitAll()
                        // Bulk Products — public GET, authenticated write
                        .requestMatchers(HttpMethod.GET, "/api/v1/bulk-products").permitAll()
                        .requestMatchers("/api/v1/bulk-products/**").authenticated()
                        // Negotiations — authenticated access (method-level @PreAuthorize handles
                        // roles)
                        .requestMatchers("/api/v1/negotiations/**").authenticated()
                        // Events — farmer read, admin write
                        .requestMatchers("/api/v1/events/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/events").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/**").authenticated()
                        .requestMatchers("/api/v1/events/**").authenticated()
                        // Role-Based Access Control paths
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/farmer/**").hasRole("FARMER")
                        .requestMatchers("/api/v1/wholesaler/**").hasRole("WHOLESALER")
                        .requestMatchers("/api/v1/delivery/**").hasRole("DELIVERY")
                        .requestMatchers("/api/v1/user/**").hasRole("USER")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.getWriter().write("{\"error\":\"Unauthorized: please login to continue\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.getWriter().write("{\"error\":\"Forbidden: admin role required\"}");
                        }))
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            String redirectUrl = frontendUrl + "/?oauth2=error&message="
                                    + java.net.URLEncoder.encode("Google login failed",
                                            java.nio.charset.StandardCharsets.UTF_8);
                            response.sendRedirect(redirectUrl);
                        }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
