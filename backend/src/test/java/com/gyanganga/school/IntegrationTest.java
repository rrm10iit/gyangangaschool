package com.gyanganga.school;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyanganga.school.model.AppUser;
import com.gyanganga.school.model.Enums.Role;
import com.gyanganga.school.repo.Repos;
import com.gyanganga.school.security.JwtService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Starts the whole application against a real PostgreSQL (Testcontainers). One container and one
 * application context are shared by every subclass. Needs Docker.
 */
@SpringBootTest(properties = {
        "app.jwt-secret=integration-test-secret-integration-test-secret",
        "app.admin-email=admin@gyanganga.school",
        "app.admin-password=Admin-Test-123",
        "app.upload-dir=target/it-data",
        "app.seed-demo-data=false",
        "app.cookie-secure=false"
})
@AutoConfigureMockMvc
@Import(IntegrationTest.Containers.class)
public abstract class IntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:16-alpine");
        }
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper om;
    @Autowired protected JwtService jwt;
    @Autowired protected Repos.UserRepo users;
    @Autowired protected Repos.EnquiryRepo enquiries;
    @Autowired protected PasswordEncoder encoder;

    private static final AtomicInteger IP = new AtomicInteger(1);

    /** A different client address per call, so rate limits in one test never affect another. */
    protected static String freshIp() {
        int n = IP.getAndIncrement();
        return "10." + (n / 65025 % 255) + "." + (n / 255 % 255) + "." + (n % 255 + 1);
    }

    protected static String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /** Authorization header value for a (created on demand) user with the given role. */
    protected String bearer(Role role) {
        String email = role.name().toLowerCase() + "-it@example.com";
        if (!users.existsByEmailIgnoreCase(email)) {
            AppUser u = new AppUser();
            u.setEmail(email);
            u.setFullName(role + " Tester");
            u.setRole(role);
            u.setPasswordHash(encoder.encode("irrelevant-password"));
            users.save(u);
        }
        return "Bearer " + jwt.generate(email, role.name());
    }

    protected MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder b, String auth) {
        return auth == null ? b : b.header(HttpHeaders.AUTHORIZATION, auth);
    }

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, Object body) throws Exception {
        return b.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body));
    }

    protected JsonNode body(MvcResult r) throws Exception {
        return om.readTree(r.getResponse().getContentAsString());
    }

    protected JsonNode postJson(String url, String auth, Object body, int expectedStatus) throws Exception {
        MvcResult r = mvc.perform(as(json(MockMvcRequestBuilders.post(url), body), auth)).andReturn();
        org.assertj.core.api.Assertions.assertThat(r.getResponse().getStatus())
                .as("POST %s -> %s", url, r.getResponse().getContentAsString()).isEqualTo(expectedStatus);
        return r.getResponse().getContentAsString().isBlank() ? null : body(r);
    }

    /** GET; {@code vars} fill {placeholders} in the url and are encoded safely. */
    protected JsonNode get(String url, String auth, int expectedStatus, Object... vars) throws Exception {
        MvcResult r = mvc.perform(as(MockMvcRequestBuilders.get(url, vars), auth)).andReturn();
        org.assertj.core.api.Assertions.assertThat(r.getResponse().getStatus())
                .as("GET %s -> %s", url, r.getResponse().getContentAsString()).isEqualTo(expectedStatus);
        String text = r.getResponse().getContentAsString();
        return text.isBlank() || !r.getResponse().getContentType().contains("json") ? null : om.readTree(text);
    }

    /** Creates a class so a test has its own fee structure and students. */
    protected long createClass(String adminAuth) throws Exception {
        return postJson("/api/admin/classes", adminAuth,
                Map.of("name", "Class-" + uid(), "displayOrder", 500, "sections", "A"), 200).get("id").asLong();
    }

    protected long createStudent(String adminAuth, long classId, Map<String, Object> extra) throws Exception {
        java.util.Map<String, Object> b = new java.util.HashMap<>(Map.<String, Object>of(
                "fullName", "Student " + uid(), "admissionNo", "ADM-" + uid(), "classId", classId));
        b.putAll(extra);
        return postJson("/api/admin/students", adminAuth, b, 200).get("id").asLong();
    }
}
