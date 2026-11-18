package com.gyanganga.school;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.gyanganga.school.model.Enums.Role;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SchoolDataIT extends IntegrationTest {

    private String admin() {
        return bearer(Role.ADMIN);
    }

    @Test
    void studentValidationReportsEachBadField() throws Exception {
        long classId = createClass(admin());
        Map<String, Object> bad = new HashMap<>();
        bad.put("fullName", "");
        bad.put("admissionNo", "A-" + uid());
        bad.put("classId", classId);
        bad.put("guardianEmail", "not-an-email");
        JsonNode r = postJson("/api/admin/students", admin(), bad, 400);
        assertThat(r.get("fields").has("fullName")).isTrue();
        assertThat(r.get("fields").has("guardianEmail")).isTrue();
        assertThat(r.get("fields").has("admissionNo")).isFalse();
    }

    @Test
    void studentMustBelongToAnExistingClass() throws Exception {
        postJson("/api/admin/students", admin(),
                Map.of("fullName", "X", "admissionNo", "A-" + uid(), "classId", 999999999L), 400);
    }

    @Test
    void admissionNumbersAreUniqueOnCreateAndUpdate() throws Exception {
        long classId = createClass(admin());
        String adm = "DUP-" + uid();
        createStudent(admin(), classId, Map.of("admissionNo", adm));
        postJson("/api/admin/students", admin(),
                Map.of("fullName", "Other", "admissionNo", adm, "classId", classId), 409);

        long second = createStudent(admin(), classId, Map.of());
        JsonNode existing = get("/api/admin/students/" + second, admin(), 200);
        Map<String, Object> change = om.convertValue(existing, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        change.put("admissionNo", adm);
        mvc.perform(json(put("/api/admin/students/" + second), change).header("Authorization", admin()))
                .andExpect(status().isConflict());
    }

    @Test
    void updateKeepsTheSameAdmissionNumberAndChangesTheOtherFields() throws Exception {
        long classId = createClass(admin());
        long id = createStudent(admin(), classId, Map.of("section", "A"));
        Map<String, Object> s = om.convertValue(get("/api/admin/students/" + id, admin(), 200),
                new com.fasterxml.jackson.core.type.TypeReference<>() {});
        s.put("section", "B");
        s.put("guardianName", "Changed Guardian");
        mvc.perform(json(put("/api/admin/students/" + id), s).header("Authorization", admin())).andExpect(status().isOk());
        JsonNode after = get("/api/admin/students/" + id, admin(), 200);
        assertThat(after.get("section").asText()).isEqualTo("B");
        assertThat(after.get("guardianName").asText()).isEqualTo("Changed Guardian");
        assertThat(after.get("admissionNo").asText()).isEqualTo(s.get("admissionNo"));
    }

    @Test
    void searchIsCaseInsensitiveFiltersByClassAndTreatsInputAsPlainText() throws Exception {
        long classId = createClass(admin());
        String marker = "Quokka" + uid();
        long id = createStudent(admin(), classId, Map.of("fullName", "Zebra " + marker));
        createStudent(admin(), classId, Map.of());

        JsonNode byName = get("/api/admin/students?q=" + marker.toLowerCase(), admin(), 200);
        assertThat(byName.get("total").asInt()).isEqualTo(1);
        assertThat(byName.get("content").get(0).get("id").asLong()).isEqualTo(id);

        assertThat(get("/api/admin/students?classId=" + classId, admin(), 200).get("total").asInt()).isEqualTo(2);

        // wildcards and SQL fragments are data, not syntax
        assertThat(get("/api/admin/students?q={q}", admin(), 200, "%").get("total").asInt()).isZero();
        assertThat(get("/api/admin/students?q={q}", admin(), 200, "_").get("total").asInt()).isZero();
        get("/api/admin/students?q={q}", admin(), 200, "'; DROP TABLE student; --");
        assertThat(get("/api/admin/students?classId=" + classId, admin(), 200).get("total").asInt()).isEqualTo(2);
    }

    @Test
    void paginationSizeIsCapped() throws Exception {
        assertThat(get("/api/admin/students?size=100000", admin(), 200).get("size").asInt()).isEqualTo(500);
        assertThat(get("/api/admin/students?size=0", admin(), 200).get("size").asInt()).isEqualTo(1);
        get("/api/admin/students?classId=abc", admin(), 400);
    }

    @Test
    void aClassWithStudentsCannotBeDeletedButAnEmptyOneCan() throws Exception {
        long classId = createClass(admin());
        long studentId = createStudent(admin(), classId, Map.of());
        mvc.perform(delete("/api/admin/classes/" + classId).header("Authorization", admin())).andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/students/" + studentId).header("Authorization", admin())).andExpect(status().isOk());
        mvc.perform(delete("/api/admin/classes/" + classId).header("Authorization", admin())).andExpect(status().isOk());
        get("/api/admin/students/" + studentId, admin(), 404);
    }

    @Test
    void teacherEmployeeNumbersAreUnique() throws Exception {
        String emp = "EMP-" + uid();
        postJson("/api/admin/teachers", admin(), Map.of("fullName", "T One", "employeeNo", emp), 200);
        postJson("/api/admin/teachers", admin(), Map.of("fullName", "T Two", "employeeNo", emp), 409);
        postJson("/api/admin/teachers", admin(), Map.of("fullName", "", "employeeNo", "E-" + uid()), 400);
    }

    @Test
    void everyChangeIsWrittenToTheActivityLogWithWhoDidIt() throws Exception {
        long classId = createClass(admin());
        String name = "Audited" + uid();
        createStudent(admin(), classId, Map.of("fullName", name));
        JsonNode log = get("/api/admin/audit-log?q=" + name, admin(), 200);
        assertThat(log.get("total").asInt()).isEqualTo(1);
        assertThat(log.get("content").get(0).get("userEmail").asText()).isEqualTo("admin-it@example.com");
        assertThat(log.get("content").get(0).get("action").asText()).isEqualTo("CREATE");
    }

    @Test
    void activityLogIsReadOnly() throws Exception {
        postJson("/api/admin/audit-log", admin(), Map.of("action", "FAKE"), 405);
        mvc.perform(delete("/api/admin/audit-log/1").header("Authorization", admin())).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unknownAdminUrlsAre404NotServerErrors() throws Exception {
        get("/api/admin/does-not-exist", admin(), 404);
    }
}
