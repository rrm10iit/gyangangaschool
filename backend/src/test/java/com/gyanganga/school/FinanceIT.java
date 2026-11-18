package com.gyanganga.school;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.gyanganga.school.model.Enums.Role;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class FinanceIT extends IntegrationTest {

    private String admin() {
        return bearer(Role.ADMIN);
    }

    private String accountant() {
        return bearer(Role.ACCOUNTANT);
    }

    private void head(long classId, String name, int amount, String frequency) throws Exception {
        postJson("/api/admin/finance/fee-structure", accountant(),
                Map.of("classId", classId, "headName", name, "amount", amount, "frequency", frequency), 200);
    }

    private JsonNode pay(long studentId, Object amount, String date) throws Exception {
        return postJson("/api/admin/finance/payments", accountant(),
                Map.of("studentId", studentId, "amount", amount, "paymentDate", date, "mode", "UPI", "forPeriod", "Test"), 200);
    }

    private static BigDecimal dec(JsonNode n) {
        return new BigDecimal(n.asText());
    }

    // ------------------------------------------------------------------ fees and ledger

    @Test
    void yearlyFeeIsTheSumOfHeadsMinusConcessionAndPaymentsReduceWhatIsPending() throws Exception {
        long classId = createClass(admin());
        head(classId, "Tuition", 100, "MONTHLY");   // 1200 a year
        head(classId, "Annual", 500, "ANNUAL");     // 500 a year
        long full = createStudent(admin(), classId, Map.of());
        long discounted = createStudent(admin(), classId, Map.of("feeDiscount", 200));

        assertThat(dec(get("/api/admin/finance/students/" + full + "/ledger", accountant(), 200).get("totalFee"))).isEqualByComparingTo("1700");
        JsonNode ledger = get("/api/admin/finance/students/" + discounted + "/ledger", accountant(), 200);
        assertThat(dec(ledger.get("totalFee"))).isEqualByComparingTo("1500");
        assertThat(dec(ledger.get("pending"))).isEqualByComparingTo("1500");

        pay(discounted, 500, LocalDate.now().toString());
        ledger = get("/api/admin/finance/students/" + discounted + "/ledger", accountant(), 200);
        assertThat(dec(ledger.get("paid"))).isEqualByComparingTo("500");
        assertThat(dec(ledger.get("pending"))).isEqualByComparingTo("1000");
        assertThat(ledger.get("payments")).hasSize(1);
    }

    @Test
    void duesListShowsOnlyStudentsWhoStillOweAndDropsThemOncePaidInFull() throws Exception {
        long classId = createClass(admin());
        head(classId, "Annual", 1000, "ANNUAL");
        long id = createStudent(admin(), classId, Map.of());

        JsonNode dues = get("/api/admin/finance/dues?classId=" + classId, accountant(), 200);
        assertThat(dues).hasSize(1);
        assertThat(dues.get(0).get("studentId").asLong()).isEqualTo(id);
        assertThat(dec(dues.get(0).get("pending"))).isEqualByComparingTo("1000");
        // an annual head is due from the first month, so it is overdue as well
        assertThat(dec(dues.get(0).get("overdue"))).isEqualByComparingTo("1000");

        pay(id, 1000, LocalDate.now().toString());
        assertThat(get("/api/admin/finance/dues?classId=" + classId, accountant(), 200)).isEmpty();
    }

    @Test
    void studentsWhoLeftAreNotChasedForFees() throws Exception {
        long classId = createClass(admin());
        head(classId, "Annual", 1000, "ANNUAL");
        createStudent(admin(), classId, Map.of("status", "LEFT"));
        assertThat(get("/api/admin/finance/dues?classId=" + classId, accountant(), 200)).isEmpty();
    }

    // ------------------------------------------------------------------ payments

    @Test
    void receiptNumbersAreUniqueSequentialAndWellFormed() throws Exception {
        long classId = createClass(admin());
        long id = createStudent(admin(), classId, Map.of());
        String first = pay(id, 10, LocalDate.now().toString()).get("receiptNo").asText();
        String second = pay(id, 10, LocalDate.now().toString()).get("receiptNo").asText();
        assertThat(first).matches("GGS/\\d{4}-\\d{2}/\\d{6}");
        assertThat(second).matches("GGS/\\d{4}-\\d{2}/\\d{6}").isNotEqualTo(first);
        assertThat(second.compareTo(first)).isPositive();
    }

    @Test
    void paymentsAreValidated() throws Exception {
        long classId = createClass(admin());
        long id = createStudent(admin(), classId, Map.of());
        String today = LocalDate.now().toString();
        postJson("/api/admin/finance/payments", accountant(),
                Map.of("studentId", id, "amount", 0, "paymentDate", today, "mode", "CASH"), 400);
        postJson("/api/admin/finance/payments", accountant(),
                Map.of("studentId", id, "amount", -50, "paymentDate", today, "mode", "CASH"), 400);
        postJson("/api/admin/finance/payments", accountant(),
                Map.of("studentId", id, "amount", 50, "paymentDate", LocalDate.now().plusDays(10).toString(), "mode", "CASH"), 400);
        postJson("/api/admin/finance/payments", accountant(),
                Map.of("studentId", 999999999L, "amount", 50, "paymentDate", today, "mode", "CASH"), 400);
        postJson("/api/admin/finance/payments", accountant(),
                Map.of("studentId", id, "amount", 50, "paymentDate", today, "mode", "BITCOIN"), 400);
    }

    @Test
    void paymentFallsInTheAcademicYearOfItsDate() throws Exception {
        long classId = createClass(admin());
        long id = createStudent(admin(), classId, Map.of());
        // 31 March belongs to the year that started the previous April; 1 April starts a new one
        JsonNode march = pay(id, 10, LocalDate.of(LocalDate.now().getYear() - 1, 3, 31).toString());
        JsonNode april = pay(id, 10, LocalDate.of(LocalDate.now().getYear() - 1, 4, 1).toString());
        assertThat(march.get("academicYear").asInt()).isEqualTo(LocalDate.now().getYear() - 2);
        assertThat(april.get("academicYear").asInt()).isEqualTo(LocalDate.now().getYear() - 1);
    }

    @Test
    void cancellingAReceiptRemovesItFromTheLedgerAndIsLogged() throws Exception {
        long classId = createClass(admin());
        head(classId, "Annual", 1000, "ANNUAL");
        long id = createStudent(admin(), classId, Map.of());
        JsonNode p = pay(id, 400, LocalDate.now().toString());

        mvc.perform(delete("/api/admin/finance/payments/" + p.get("id").asLong()).header("Authorization", admin()))
                .andExpect(status().isOk());
        assertThat(dec(get("/api/admin/finance/students/" + id + "/ledger", accountant(), 200).get("paid"))).isEqualByComparingTo("0");
        JsonNode log = get("/api/admin/audit-log?q={q}", admin(), 200, p.get("receiptNo").asText());
        assertThat(log.get("content").toString()).contains("Cancelled receipt");
    }

    @Test
    void aStudentWithPaymentsCannotBeDeleted() throws Exception {
        long classId = createClass(admin());
        long id = createStudent(admin(), classId, Map.of());
        pay(id, 10, LocalDate.now().toString());
        mvc.perform(delete("/api/admin/students/" + id).header("Authorization", admin())).andExpect(status().isConflict());
    }

    @Test
    void receiptIsAPdfWithTheReceiptNumberAndPaymentAmountEndpointsRequireFinanceAccess() throws Exception {
        long classId = createClass(admin());
        long id = createStudent(admin(), classId, Map.of());
        long paymentId = pay(id, 1234.5, LocalDate.now().toString()).get("id").asLong();
        MvcResult r = mvc.perform(MockMvcRequestBuilders.get("/api/admin/finance/payments/" + paymentId + "/receipt.pdf")
                .header("Authorization", accountant())).andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(new String(r.getResponse().getContentAsByteArray(), 0, 5)).isEqualTo("%PDF-");
        mvc.perform(MockMvcRequestBuilders.get("/api/admin/finance/payments/" + paymentId + "/receipt.pdf")
                .header("Authorization", bearer(Role.TEACHER))).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ salaries and expenses

    @Test
    void salaryNetIsBasePlusAllowancesMinusDeductionsAndOneRecordPerMonth() throws Exception {
        long teacherId = postJson("/api/admin/teachers", admin(), Map.of("fullName", "Paid " + uid(), "employeeNo", "S-" + uid()), 200)
                .get("id").asLong();
        Map<String, Object> salary = Map.of("teacherId", teacherId, "month", "2026-05", "baseAmount", 1000,
                "allowances", 200, "deductions", 50, "status", "PENDING");
        JsonNode created = postJson("/api/admin/finance/salaries", accountant(), salary, 200);
        assertThat(dec(created.get("netAmount"))).isEqualByComparingTo("1150");
        assertThat(created.has("paidDate")).isFalse();
        postJson("/api/admin/finance/salaries", accountant(), salary, 409);

        // marking as paid fills in a date
        Map<String, Object> paid = new java.util.HashMap<>(salary);
        paid.put("month", "2026-06");
        paid.put("status", "PAID");
        JsonNode p = postJson("/api/admin/finance/salaries", accountant(), paid, 200);
        assertThat(p.get("paidDate").asText()).isEqualTo(LocalDate.now().toString());
    }

    @Test
    void salaryValidation() throws Exception {
        long teacherId = postJson("/api/admin/teachers", admin(), Map.of("fullName", "V " + uid(), "employeeNo", "V-" + uid()), 200)
                .get("id").asLong();
        postJson("/api/admin/finance/salaries", accountant(),
                Map.of("teacherId", teacherId, "month", "2026-13", "baseAmount", 100, "status", "PENDING"), 400);
        postJson("/api/admin/finance/salaries", accountant(),
                Map.of("teacherId", teacherId, "month", "2026-07", "baseAmount", 100, "deductions", 500, "status", "PENDING"), 400);
        postJson("/api/admin/finance/salaries", accountant(),
                Map.of("teacherId", 999999999L, "month", "2026-07", "baseAmount", 100, "status", "PENDING"), 400);
    }

    @Test
    void expensesAreRecordedWithWhoAddedThemAndCanBeFiltered() throws Exception {
        String category = "Cat" + uid();
        JsonNode e = postJson("/api/admin/finance/expenses", accountant(), Map.of(
                "category", category, "description", "Chalk", "amount", 250, "expenseDate", LocalDate.now().toString()), 200);
        assertThat(e.get("createdBy").asText()).isEqualTo("accountant-it@example.com");
        assertThat(get("/api/admin/finance/expenses?category=" + category, accountant(), 200).get("total").asInt()).isEqualTo(1);
        postJson("/api/admin/finance/expenses", accountant(), Map.of(
                "category", category, "description", "Free", "amount", 0, "expenseDate", LocalDate.now().toString()), 400);
    }

    // ------------------------------------------------------------------ reports

    @Test
    void reportAddsUpPaymentsSalariesAndExpensesForTheMonth() throws Exception {
        long classId = createClass(admin());
        long student = createStudent(admin(), classId, Map.of());
        LocalDate today = LocalDate.now();
        int year = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
        String url = "/api/admin/finance/reports/summary?year=" + year + "&month=" + today.getMonthValue();

        JsonNode before = get(url, accountant(), 200);
        pay(student, 700, today.toString());
        postJson("/api/admin/finance/expenses", accountant(), Map.of(
                "category", "Test", "description", "x", "amount", 300, "expenseDate", today.toString()), 200);
        JsonNode after = get(url, accountant(), 200);

        assertThat(dec(after.get("income")).subtract(dec(before.get("income")))).isEqualByComparingTo("700");
        assertThat(dec(after.get("expenses")).subtract(dec(before.get("expenses")))).isEqualByComparingTo("300");
        assertThat(dec(after.get("net"))).isEqualByComparingTo(
                dec(after.get("income")).subtract(dec(after.get("salaries"))).subtract(dec(after.get("expenses"))));
        assertThat(after.get("series")).isNotEmpty();
    }

    @Test
    void yearlyReportHasTwelveMonthsStartingInApril() throws Exception {
        JsonNode r = get("/api/admin/finance/reports/summary?year=2026", accountant(), 200);
        assertThat(r.get("series")).hasSize(12);
        assertThat(r.get("series").get(0).get("label").asText()).isEqualTo("Apr 2026");
        assertThat(r.get("series").get(11).get("label").asText()).isEqualTo("Mar 2027");
        assertThat(r.get("academicYearLabel").asText()).isEqualTo("2026-27");
        get("/api/admin/finance/reports/summary?year=2026&month=13", accountant(), 400);
    }

    @Test
    void reportsCanBeExportedAsExcelAndPdf() throws Exception {
        MvcResult xlsx = mvc.perform(MockMvcRequestBuilders.get("/api/admin/finance/reports/export.xlsx?year=2026").header("Authorization", accountant()))
                .andExpect(status().isOk()).andReturn();
        byte[] x = xlsx.getResponse().getContentAsByteArray();
        assertThat(new String(x, 0, 2)).isEqualTo("PK"); // xlsx is a zip
        assertThat(xlsx.getResponse().getHeader("Content-Disposition")).contains("finance-report-2026-27.xlsx");

        MvcResult pdf = mvc.perform(MockMvcRequestBuilders.get("/api/admin/finance/reports/export.pdf?year=2026&month=4").header("Authorization", accountant()))
                .andExpect(status().isOk()).andReturn();
        assertThat(new String(pdf.getResponse().getContentAsByteArray(), 0, 5)).isEqualTo("%PDF-");
    }

    // ------------------------------------------------------------------ dashboard

    @Test
    void dashboardHidesMoneyFromTeachers() throws Exception {
        assertThat(get("/api/admin/dashboard", bearer(Role.TEACHER), 200).has("finance")).isFalse();
        assertThat(get("/api/admin/dashboard", bearer(Role.TEACHER), 200).has("newEnquiries")).isFalse();
        JsonNode a = get("/api/admin/dashboard", admin(), 200);
        assertThat(a.has("finance")).isTrue();
        assertThat(a.get("finance").get("series")).hasSize(12);
        assertThat(get("/api/admin/dashboard", accountant(), 200).has("finance")).isTrue();
    }
}
