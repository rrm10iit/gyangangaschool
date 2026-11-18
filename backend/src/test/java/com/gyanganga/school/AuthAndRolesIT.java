package com.gyanganga.school;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gyanganga.school.model.Enums.Role;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MvcResult;

class AuthAndRolesIT extends IntegrationTest {

    // ------------------------------------------------------------------ login

    @Test
    void loginSetsAnHttpOnlySameSiteCookieAndCookieAuthenticates() throws Exception {
        MvcResult r = mvc.perform(json(post("/api/auth/login"),
                        Map.of("email", "ADMIN@gyanganga.school", "password", "Admin-Test-123"))
                        .with(req -> { req.setRemoteAddr(freshIp()); return req; }))
                .andExpect(status().isOk()).andReturn();
        String setCookie = r.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).contains("ggs_token=").contains("HttpOnly").contains("SameSite=Strict");
        assertThat(body(r).get("role").asText()).isEqualTo("ADMIN");
        assertThat(body(r).has("passwordHash")).isFalse();

        String token = setCookie.substring("ggs_token=".length(), setCookie.indexOf(';'));
        mvc.perform(MockMvcRequestBuilders.get("/api/auth/me").cookie(new Cookie("ggs_token", token))).andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameAnswer() throws Exception {
        String ip = freshIp();
        MvcResult wrong = mvc.perform(json(post("/api/auth/login"),
                Map.of("email", "admin@gyanganga.school", "password", "nope"))
                .with(req -> { req.setRemoteAddr(ip); return req; })).andExpect(status().isUnauthorized()).andReturn();
        MvcResult unknown = mvc.perform(json(post("/api/auth/login"),
                Map.of("email", "nobody-" + uid() + "@example.com", "password", "nope"))
                .with(req -> { req.setRemoteAddr(ip); return req; })).andExpect(status().isUnauthorized()).andReturn();
        assertThat(body(wrong).get("message")).isEqualTo(body(unknown).get("message"));
    }

    @Test
    void accountIsLockedAfterFiveFailedAttempts() throws Exception {
        String email = "lock-" + uid() + "@example.com";
        postJson("/api/admin/users", bearer(Role.ADMIN),
                Map.of("email", email, "fullName", "Lock Test", "role", "TEACHER", "enabled", true,
                        "password", "CorrectPass123"), 200);
        for (int i = 0; i < 5; i++) {
            mvc.perform(json(post("/api/auth/login"), Map.of("email", email, "password", "bad" + i))
                    .with(req -> { req.setRemoteAddr(freshIp()); return req; })).andExpect(status().isUnauthorized());
        }
        // even the correct password is refused while locked
        mvc.perform(json(post("/api/auth/login"), Map.of("email", email, "password", "CorrectPass123"))
                .with(req -> { req.setRemoteAddr(freshIp()); return req; })).andExpect(status().isTooManyRequests());
    }

    @Test
    void disabledAccountsCannotSignInAndExistingTokensStopWorking() throws Exception {
        String email = "off-" + uid() + "@example.com";
        JsonNodeHolder created = new JsonNodeHolder(postJson("/api/admin/users", bearer(Role.ADMIN),
                Map.of("email", email, "fullName", "Off", "role", "TEACHER", "enabled", true, "password", "CorrectPass123"), 200));
        String auth = "Bearer " + jwt.generate(email, "TEACHER");
        get("/api/admin/students", auth, 200);

        mvc.perform(json(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/users/" + created.id()), Map.of("email", email, "fullName", "Off",
                        "role", "TEACHER", "enabled", false))
                .header("Authorization", bearer(Role.ADMIN))).andExpect(status().isOk());

        get("/api/admin/students", auth, 401);
        mvc.perform(json(post("/api/auth/login"), Map.of("email", email, "password", "CorrectPass123"))
                .with(req -> { req.setRemoteAddr(freshIp()); return req; })).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedAndGarbageTokensAreRejected() throws Exception {
        String good = jwt.generate("admin@gyanganga.school", "ADMIN");
        get("/api/admin/students", "Bearer " + good + "x", 401);
        get("/api/admin/students", "Bearer not-a-jwt", 401);
        get("/api/admin/students", "Bearer ", 401);
    }

    @Test
    void roleClaimInTheTokenIsIgnoredTheDatabaseRoleWins() throws Exception {
        // a teacher whose token claims ADMIN must still be a teacher
        bearer(Role.TEACHER);
        String forged = "Bearer " + jwt.generate("teacher-it@example.com", "ADMIN");
        get("/api/admin/finance/dues", forged, 403);
    }

    @Test
    void changePasswordRequiresTheCurrentPasswordAndEnforcesLength() throws Exception {
        String email = "pw-" + uid() + "@example.com";
        postJson("/api/admin/users", bearer(Role.ADMIN),
                Map.of("email", email, "fullName", "Pw", "role", "ACCOUNTANT", "enabled", true, "password", "OldPassword123"), 200);
        String auth = "Bearer " + jwt.generate(email, "ACCOUNTANT");
        postJson("/api/auth/change-password", auth, Map.of("currentPassword", "wrong", "newPassword", "NewPassword123"), 400);
        postJson("/api/auth/change-password", auth, Map.of("currentPassword", "OldPassword123", "newPassword", "short"), 400);
        postJson("/api/auth/change-password", auth, Map.of("currentPassword", "OldPassword123", "newPassword", "NewPassword123"), 200);
        mvc.perform(json(post("/api/auth/login"), Map.of("email", email, "password", "NewPassword123"))
                .with(req -> { req.setRemoteAddr(freshIp()); return req; })).andExpect(status().isOk());
    }

    @Test
    void administratorsCannotDeleteTheirOwnAccount() throws Exception {
        String adminAuth = "Bearer " + jwt.generate("admin@gyanganga.school", "ADMIN");
        long selfId = get("/api/auth/me", adminAuth, 200).get("id").asLong();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/users/" + selfId)
                .header("Authorization", adminAuth)).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ access matrix

    static Stream<Arguments> matrix() {
        // role, method, path, expected status. A body of {} is sent on POST so only access control decides the result.
        Object[][] rows = {
                // no login
                {null, "GET", "/api/admin/students", 401},
                {null, "GET", "/api/admin/dashboard", 401},
                {null, "GET", "/api/admin/finance/dues", 401},
                {null, "GET", "/api/admin/files/00000000-0000-0000-0000-000000000000.pdf", 401},
                {null, "GET", "/api/auth/me", 401},
                // public stays public
                {null, "GET", "/api/public/site", 200},
                {null, "GET", "/api/public/classes", 200},
                {null, "GET", "/actuator/health", 200},
                // teacher: read people, nothing else
                {Role.TEACHER, "GET", "/api/admin/students", 200},
                {Role.TEACHER, "GET", "/api/admin/teachers", 200},
                {Role.TEACHER, "GET", "/api/admin/classes", 200},
                {Role.TEACHER, "GET", "/api/admin/dashboard", 200},
                {Role.TEACHER, "POST", "/api/admin/students", 403},
                {Role.TEACHER, "POST", "/api/admin/teachers", 403},
                {Role.TEACHER, "POST", "/api/admin/classes", 403},
                {Role.TEACHER, "GET", "/api/admin/finance/dues", 403},
                {Role.TEACHER, "GET", "/api/admin/finance/payments", 403},
                {Role.TEACHER, "GET", "/api/admin/finance/salaries", 403},
                {Role.TEACHER, "GET", "/api/admin/finance/reports/summary", 403},
                {Role.TEACHER, "GET", "/api/admin/settings", 403},
                {Role.TEACHER, "GET", "/api/admin/enquiries", 403},
                {Role.TEACHER, "GET", "/api/admin/users", 403},
                {Role.TEACHER, "GET", "/api/admin/audit-log", 403},
                {Role.TEACHER, "POST", "/api/admin/uploads", 403},
                // accountant: finance and read people
                {Role.ACCOUNTANT, "GET", "/api/admin/finance/dues", 200},
                {Role.ACCOUNTANT, "GET", "/api/admin/finance/payments", 200},
                {Role.ACCOUNTANT, "GET", "/api/admin/finance/salaries", 200},
                {Role.ACCOUNTANT, "GET", "/api/admin/finance/expenses", 200},
                {Role.ACCOUNTANT, "GET", "/api/admin/finance/fee-structure", 200},
                {Role.ACCOUNTANT, "GET", "/api/admin/finance/reports/summary", 200},
                {Role.ACCOUNTANT, "GET", "/api/admin/students", 200},
                {Role.ACCOUNTANT, "GET", "/api/admin/teachers", 200},
                {Role.ACCOUNTANT, "POST", "/api/admin/students", 403},
                {Role.ACCOUNTANT, "POST", "/api/admin/teachers", 403},
                {Role.ACCOUNTANT, "GET", "/api/admin/settings", 403},
                {Role.ACCOUNTANT, "GET", "/api/admin/users", 403},
                {Role.ACCOUNTANT, "GET", "/api/admin/audit-log", 403},
                {Role.ACCOUNTANT, "GET", "/api/admin/enquiries", 403},
                // admin: everything
                {Role.ADMIN, "GET", "/api/admin/students", 200},
                {Role.ADMIN, "GET", "/api/admin/finance/dues", 200},
                {Role.ADMIN, "GET", "/api/admin/settings", 200},
                {Role.ADMIN, "GET", "/api/admin/users", 200},
                {Role.ADMIN, "GET", "/api/admin/audit-log", 200},
                {Role.ADMIN, "GET", "/api/admin/enquiries", 200},
                {Role.ADMIN, "GET", "/api/admin/finance/reports/summary", 200},
                // unknown admin routes are denied rather than left open
                {Role.ADMIN, "GET", "/api/admin/does-not-exist", 404},
                {Role.ADMIN, "GET", "/api/unknown", 403},
        };
        return Stream.of(rows).map(r -> Arguments.of(r[0], r[1], r[2], r[3]));
    }

    @ParameterizedTest(name = "{0} {1} {2} -> {3}")
    @MethodSource("matrix")
    void accessMatrix(Role role, String method, String path, int expected) throws Exception {
        var builder = method.equals("POST")
                ? json(post(path), Map.of())
                : MockMvcRequestBuilders.get(path);
        if (role != null) builder.header("Authorization", bearer(role));
        mvc.perform(builder).andExpect(status().is(expected));
    }

    @Test
    void accountantCannotCancelAReceipt() throws Exception {
        String admin = bearer(Role.ADMIN);
        long classId = createClass(admin);
        long studentId = createStudent(admin, classId, Map.of());
        var pay = postJson("/api/admin/finance/payments", bearer(Role.ACCOUNTANT),
                Map.of("studentId", studentId, "amount", 100, "paymentDate", java.time.LocalDate.now().toString(), "mode", "CASH"), 200);
        long id = pay.get("id").asLong();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/finance/payments/" + id)
                .header("Authorization", bearer(Role.ACCOUNTANT))).andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/finance/payments/" + id)
                .header("Authorization", admin)).andExpect(status().isOk());
    }

    private record JsonNodeHolder(com.fasterxml.jackson.databind.JsonNode n) {
        long id() {
            return n.get("id").asLong();
        }
    }
}
