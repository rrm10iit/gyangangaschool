package com.gyanganga.school.web;

import com.gyanganga.school.common.AuditService;
import com.gyanganga.school.common.PageDto;
import com.gyanganga.school.model.*;
import com.gyanganga.school.repo.Repos;
import com.gyanganga.school.service.FinanceService;
import com.gyanganga.school.service.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Finance endpoints. Access is limited to ADMIN and ACCOUNTANT in SecurityConfig. */
public final class FinanceControllers {
    private FinanceControllers() {}

    @RestController
    @RequestMapping("/api/admin/finance/fee-structure")
    public static class FeeStructureController extends CrudController<FeeStructure> {
        private final Repos.ClassRepo classes;

        public FeeStructureController(Repos.FeeStructureRepo repo, AuditService audit, Repos.ClassRepo classes) {
            super(repo, audit, "Fee structure");
            this.classes = classes;
        }

        @Override
        protected Sort defaultSort() {
            return Sort.by("classId", "id");
        }

        @Override
        protected Specification<FeeStructure> filter(String q, Map<String, String> p) {
            return Specification.allOf(likeAny(q, "headName"), eq("classId", longParam(p, "classId")));
        }

        @Override
        protected void beforeSave(FeeStructure f, Long id) {
            if (!classes.existsById(f.getClassId())) throw bad("Selected class does not exist");
        }

        @Override
        protected String describe(FeeStructure f) {
            return "Fee head '" + f.getHeadName() + "' = " + f.getAmount() + " " + f.getFrequency() + " (class #"
                    + f.getClassId() + ")";
        }
    }

    @RestController
    @RequestMapping("/api/admin/finance/expenses")
    public static class ExpenseController extends CrudController<Expense> {
        public ExpenseController(Repos.ExpenseRepo repo, AuditService audit) {
            super(repo, audit, "Expense");
        }

        @Override
        protected Sort defaultSort() {
            return Sort.by(Sort.Direction.DESC, "expenseDate", "id");
        }

        @Override
        protected Specification<Expense> filter(String q, Map<String, String> p) {
            LocalDate from = dateParam(p, "from");
            LocalDate to = dateParam(p, "to");
            String category = p.get("category");
            return Specification.allOf(
                    likeAny(q, "description", "category", "paidTo"),
                    category == null || category.isBlank() ? null : eq("category", category),
                    from == null ? null : (Specification<Expense>) (r, qr, cb) -> cb.greaterThanOrEqualTo(r.<LocalDate>get("expenseDate"), from),
                    to == null ? null : (Specification<Expense>) (r, qr, cb) -> cb.lessThanOrEqualTo(r.<LocalDate>get("expenseDate"), to));
        }

        @Override
        protected void beforeSave(Expense e, Long id) {
            if (id == null) e.setCreatedBy(AuditService.currentUser());
        }

        @Override
        protected String[] readOnlyOnUpdate() {
            return new String[] {"createdBy"};
        }

        @Override
        protected String describe(Expense e) {
            return "Expense " + e.getCategory() + ": " + e.getDescription() + " = " + e.getAmount();
        }
    }

    @RestController
    @RequestMapping("/api/admin/finance/salaries")
    public static class SalaryController extends CrudController<SalaryRecord> {
        private final Repos.SalaryRepo salaries;
        private final Repos.TeacherRepo teachers;

        public SalaryController(Repos.SalaryRepo repo, AuditService audit, Repos.TeacherRepo teachers) {
            super(repo, audit, "Salary");
            this.salaries = repo;
            this.teachers = teachers;
        }

        @Override
        protected Sort defaultSort() {
            return Sort.by(Sort.Direction.DESC, "month", "id");
        }

        @Override
        protected Specification<SalaryRecord> filter(String q, Map<String, String> p) {
            String status = p.get("status");
            String month = p.get("month");
            Specification<SalaryRecord> st = null;
            if (status != null && !status.isBlank()) {
                try {
                    st = eq("status", Enums.SalaryStatus.valueOf(status));
                } catch (IllegalArgumentException e) {
                    throw bad("Invalid status");
                }
            }
            return Specification.allOf(
                    eq("teacherId", longParam(p, "teacherId")),
                    month == null || month.isBlank() ? null : eq("month", month),
                    st);
        }

        @Override
        protected void beforeSave(SalaryRecord s, Long id) {
            if (!teachers.existsById(s.getTeacherId())) throw bad("Selected teacher does not exist");
            boolean dup = id == null
                    ? salaries.existsByTeacherIdAndMonth(s.getTeacherId(), s.getMonth())
                    : salaries.existsByTeacherIdAndMonthAndIdNot(s.getTeacherId(), s.getMonth(), id);
            if (dup) throw conflict("A salary record for this teacher and month already exists");
            if (s.getAllowances() == null) s.setAllowances(BigDecimal.ZERO);
            if (s.getDeductions() == null) s.setDeductions(BigDecimal.ZERO);
            BigDecimal net = s.getBaseAmount().add(s.getAllowances()).subtract(s.getDeductions());
            if (net.signum() < 0) throw bad("Deductions cannot exceed the salary");
            s.setNetAmount(net);
            if (s.getStatus() == null) s.setStatus(Enums.SalaryStatus.PENDING);
            if (s.getStatus() == Enums.SalaryStatus.PAID) {
                if (s.getPaidDate() == null) s.setPaidDate(LocalDate.now());
                if (s.getMode() == null) s.setMode(Enums.PayMode.BANK);
            } else {
                s.setPaidDate(null);
            }
        }

        @Override
        protected String describe(SalaryRecord s) {
            return "Salary " + s.getMonth() + " teacher #" + s.getTeacherId() + " net " + s.getNetAmount() + " ("
                    + s.getStatus() + ")";
        }
    }

    // ------------------------------------------------------------------ payments, dues, reports

    public record PaymentRequest(
            @NotNull Long studentId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull LocalDate paymentDate,
            @NotNull Enums.PayMode mode,
            @Size(max = 100) String forPeriod,
            @Size(max = 100) String reference,
            @Size(max = 300) String remarks) {}

    public record PaymentView(Long id, String receiptNo, Long studentId, String studentName, String admissionNo,
                              String className, String section, BigDecimal amount, LocalDate paymentDate,
                              int academicYear, Enums.PayMode mode, String forPeriod, String reference,
                              String remarks, String collectedBy) {}

    @RestController
    @RequestMapping("/api/admin/finance")
    public static class PaymentController {
        private final Repos.PaymentRepo payments;
        private final Repos.StudentRepo students;
        private final Repos.ClassRepo classes;
        private final FinanceService finance;
        private final ReportService reports;
        private final AuditService audit;

        public PaymentController(Repos.PaymentRepo payments, Repos.StudentRepo students, Repos.ClassRepo classes,
                                 FinanceService finance, ReportService reports, AuditService audit) {
            this.payments = payments;
            this.students = students;
            this.classes = classes;
            this.finance = finance;
            this.reports = reports;
            this.audit = audit;
        }

        @GetMapping("/payments")
        @Transactional(readOnly = true)
        public PageDto<PaymentView> list(@RequestParam(defaultValue = "") String q,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "25") int size,
                                         @RequestParam Map<String, String> params) {
            LocalDate from = CrudController.dateParam(params, "from");
            LocalDate to = CrudController.dateParam(params, "to");
            Long studentId = CrudController.longParam(params, "studentId");
            String mode = params.get("mode");
            Specification<FeePayment> modeSpec = null;
            if (mode != null && !mode.isBlank()) {
                try {
                    modeSpec = CrudController.eq("mode", Enums.PayMode.valueOf(mode));
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mode");
                }
            }
            Specification<FeePayment> qSpec = null;
            if (!q.isBlank()) {
                List<Long> ids = students.findAll(
                        CrudController.<Student>likeAny(q.trim(), "fullName", "admissionNo"), PageRequest.of(0, 500))
                        .map(Student::getId).getContent();
                String like = "%" + q.trim().toLowerCase().replace("%", "").replace("_", "") + "%";
                qSpec = (r, qr, cb) -> ids.isEmpty()
                        ? cb.like(cb.lower(r.get("receiptNo")), like)
                        : cb.or(cb.like(cb.lower(r.get("receiptNo")), like), r.get("studentId").in(ids));
            }
            Specification<FeePayment> spec = Specification.allOf(
                    qSpec, modeSpec, CrudController.eq("studentId", studentId),
                    from == null ? null : (Specification<FeePayment>) (r, qr, cb) -> cb.greaterThanOrEqualTo(r.<LocalDate>get("paymentDate"), from),
                    to == null ? null : (Specification<FeePayment>) (r, qr, cb) -> cb.lessThanOrEqualTo(r.<LocalDate>get("paymentDate"), to));
            var p = payments.findAll(spec, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                    Sort.by(Sort.Direction.DESC, "paymentDate", "id")));
            return new PageDto<>(views(p.getContent()), p.getTotalElements(), p.getNumber(), p.getSize());
        }

        @PostMapping("/payments")
        @Transactional
        public PaymentView create(@Valid @RequestBody PaymentRequest req) {
            Student s = students.findById(req.studentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student not found"));
            if (req.paymentDate().isAfter(LocalDate.now().plusDays(1))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment date cannot be in the future");
            }
            int year = FinanceService.academicYearOf(req.paymentDate());
            FeePayment p = new FeePayment();
            p.setStudentId(s.getId());
            p.setAmount(req.amount());
            p.setPaymentDate(req.paymentDate());
            p.setAcademicYear(year);
            p.setMode(req.mode());
            p.setForPeriod(blank(req.forPeriod()));
            p.setReference(blank(req.reference()));
            p.setRemarks(blank(req.remarks()));
            p.setCollectedBy(AuditService.currentUser());
            p.setReceiptNo("GGS/" + FinanceService.academicYearLabel(year) + "/"
                    + String.format("%06d", payments.nextReceiptNumber()));
            payments.save(p);
            audit.log("CREATE", "Fee payment", p.getId(),
                    "Receipt " + p.getReceiptNo() + ": " + p.getAmount() + " from " + s.getFullName());
            return views(List.of(p)).get(0);
        }

        /** Receipts are financial records: only an administrator may cancel one. */
        @DeleteMapping("/payments/{id}")
        @Transactional
        public void delete(@PathVariable Long id, Authentication auth) {
            if (auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an administrator can cancel a receipt");
            }
            FeePayment p = payments.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
            payments.delete(p);
            audit.log("DELETE", "Fee payment", id, "Cancelled receipt " + p.getReceiptNo() + " (" + p.getAmount() + ")");
        }

        @GetMapping("/payments/{id}/receipt.pdf")
        @Transactional(readOnly = true)
        public ResponseEntity<byte[]> receipt(@PathVariable Long id) {
            FeePayment p = payments.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
            PaymentView v = views(List.of(p)).get(0);
            byte[] pdf = reports.receiptPdf(v);
            String name = "receipt-" + p.getReceiptNo().replace('/', '-') + ".pdf";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(name).build().toString())
                    .body(pdf);
        }

        @GetMapping("/dues")
        public List<FinanceService.DueRow> dues(@RequestParam(required = false) Long classId,
                                                @RequestParam(required = false) Integer year,
                                                @RequestParam(defaultValue = "") String q,
                                                @RequestParam(defaultValue = "false") boolean overdueOnly) {
            int y = year == null ? FinanceService.academicYearOf(LocalDate.now()) : year;
            return finance.dues(classId, y, q).stream()
                    .filter(r -> overdueOnly ? r.overdue().signum() > 0 : r.pending().signum() > 0)
                    .toList();
        }

        @GetMapping("/students/{id}/ledger")
        public FinanceService.Ledger ledger(@PathVariable Long id, @RequestParam(required = false) Integer year) {
            Student s = students.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
            return finance.ledger(s, year == null ? FinanceService.academicYearOf(LocalDate.now()) : year);
        }

        @GetMapping("/reports/summary")
        public ReportService.Summary summary(@RequestParam(required = false) Integer year,
                                             @RequestParam(required = false) Integer month) {
            return reports.summary(year == null ? FinanceService.academicYearOf(LocalDate.now()) : year, month);
        }

        @GetMapping("/reports/export.xlsx")
        public ResponseEntity<byte[]> exportExcel(@RequestParam(required = false) Integer year,
                                                  @RequestParam(required = false) Integer month) {
            int y = year == null ? FinanceService.academicYearOf(LocalDate.now()) : year;
            byte[] data = reports.excel(y, month);
            return download(data, "finance-report-" + ReportService.fileTag(y, month) + ".xlsx",
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        @GetMapping("/reports/export.pdf")
        public ResponseEntity<byte[]> exportPdf(@RequestParam(required = false) Integer year,
                                                @RequestParam(required = false) Integer month) {
            int y = year == null ? FinanceService.academicYearOf(LocalDate.now()) : year;
            byte[] data = reports.pdf(y, month);
            return download(data, "finance-report-" + ReportService.fileTag(y, month) + ".pdf",
                    MediaType.APPLICATION_PDF);
        }

        private static ResponseEntity<byte[]> download(byte[] data, String name, MediaType type) {
            return ResponseEntity.ok().contentType(type)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                    .body(data);
        }

        private List<PaymentView> views(List<FeePayment> list) {
            Map<Long, Student> st = students.findAllById(
                    list.stream().map(FeePayment::getStudentId).distinct().toList()).stream()
                    .collect(Collectors.toMap(Student::getId, Function.identity()));
            Map<Long, String> cn = new HashMap<>();
            classes.findAll().forEach(c -> cn.put(c.getId(), c.getName()));
            return list.stream().map(p -> {
                Student s = st.get(p.getStudentId());
                return new PaymentView(p.getId(), p.getReceiptNo(), p.getStudentId(),
                        s == null ? "" : s.getFullName(), s == null ? "" : s.getAdmissionNo(),
                        s == null ? "" : cn.getOrDefault(s.getClassId(), ""), s == null ? "" : s.getSection(),
                        p.getAmount(), p.getPaymentDate(), p.getAcademicYear(), p.getMode(), p.getForPeriod(),
                        p.getReference(), p.getRemarks(), p.getCollectedBy());
            }).toList();
        }

        private static String blank(String s) {
            return s == null || s.isBlank() ? null : s.trim();
        }
    }
}
