package com.gyanganga.school.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProps(
        String jwtSecret,
        int jwtExpiryHours,
        boolean cookieSecure,
        String uploadDir,
        String adminEmail,
        String adminPassword,
        boolean seedDemoData,
        String publicUrl,
        Brevo brevo) {

    public record Brevo(String apiKey, String senderEmail, String senderName, String notifyEmail) {}
}
