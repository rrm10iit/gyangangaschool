package com.gyanganga.school.service;

import com.gyanganga.school.config.AppProps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

/** Sends transactional email through the Brevo API. Silently logs when Brevo is not configured. */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final AppProps.Brevo cfg;
    private final RestClient http = RestClient.builder().baseUrl("https://api.brevo.com/v3").build();

    public EmailService(AppProps props) {
        this.cfg = props.brevo();
    }

    public boolean enabled() {
        return notBlank(cfg.apiKey()) && notBlank(cfg.senderEmail());
    }

    public static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }

    @Async
    public void send(String toEmail, String toName, String subject, String html, String replyTo) {
        if (!enabled()) {
            log.warn("Brevo is not configured (BREVO_API_KEY / BREVO_SENDER_EMAIL); skipping email '{}'", subject);
            return;
        }
        if (!notBlank(toEmail)) return;
        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of("name", cfg.senderName(), "email", cfg.senderEmail()));
        body.put("to", List.of(Map.of("email", toEmail, "name", toName == null || toName.isBlank() ? toEmail : toName)));
        body.put("subject", subject);
        body.put("htmlContent", html);
        if (notBlank(replyTo)) body.put("replyTo", Map.of("email", replyTo));
        try {
            http.post().uri("/smtp/email").header("api-key", cfg.apiKey()).body(body).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.error("Brevo email failed: {}", e.getMessage());
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
