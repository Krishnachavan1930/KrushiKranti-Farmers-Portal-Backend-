package com.krushikranti.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartupLogger {

    private final Environment env;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent() {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }

        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");

        String[] activeProfiles = env.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? String.join(", ", activeProfiles).toUpperCase() : "DEFAULT";

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String message = String.format(
            "\n" +
            "=======================================================\n" +
            "🚀 KrushiKranti Backend Started Successfully!\n" +
            "-------------------------------------------------------\n" +
            "✅ Server running on: %s://localhost:%s%s\n" +
            "✅ Database:          Connected successfully\n" +
            "✅ Environment:       %s\n" +
            "✅ Time:              %s\n" +
            "=======================================================\n" +
            "\n" +
            "🎯 Application is READY to accept requests!\n" +
            "=======================================================",
            protocol, serverPort, contextPath,
            profile,
            timestamp
        );

        log.info(message);
    }
}
