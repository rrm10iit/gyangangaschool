package com.gyanganga.school.web;

import com.gyanganga.school.model.Enums;
import com.gyanganga.school.model.SchoolClass;
import com.gyanganga.school.repo.Repos;
import com.gyanganga.school.service.FinanceService;
import com.gyanganga.school.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {
    public record ClassCount(String name, long students) {}

    public record FinanceBlock(BigDecimal collected, BigDecimal pending, BigDecimal overdue,
                               BigDecimal collectedThisMonth, BigDecimal expensesTotal,
                               List<ReportService.SeriesPoint> series, List<ReportService.NamedAmount> byMode) {}

    public record Dashboard(long students, long teachers, long classes, Long newEnquiries, String academicYear,
                            List<ClassCount> studentsPerClass, FinanceBlock finance) {}

    private final Repos.StudentRepo students;
    private final Repos.TeacherRepo teachers;
    private final Repos.ClassRepo classes;
    private final Repos.EnquiryRepo enquiries;
    private final Repos.PaymentRepo payments;
    private final FinanceService finance;
    private final ReportService reports;

    public DashboardController(Repos.StudentRepo students, Repos.TeacherRepo teachers, Repos.ClassRepo classes,
                               Repos.EnquiryRepo enquiries, Repos.PaymentRepo payments, FinanceService finance,
                               ReportService reports) {
        this.students = students;
        this.teachers = teachers;
        this.classes = classes;
        this.enquiries = enquiries;
        this.payments = payments;
        this.finance = finance;
        this.reports = reports;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Dashboard dashboard(Authentication auth) {
        boolean admin = has(auth, "ROLE_ADMIN");
        boolean money = admin || has(auth, "ROLE_ACCOUNTANT");
        int year = FinanceService.academicYearOf(LocalDate.now());

        List<SchoolClass> all = classes.findAllByOrderByDisplayOrderAscIdAsc();
        Map<Long, Long> counts = students.countActiveByClass().stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        List<ClassCount> perClass = all.stream().map(c -> new ClassCount(c.getName(), counts.getOrDefault(c.getId(), 0L))).toList();

        FinanceBlock fb = null;
        if (money) {
            var rows = finance.dues(null, year, null);
            BigDecimal pending = rows.stream().map(FinanceService.DueRow::pending).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal overdue = rows.stream().map(FinanceService.DueRow::overdue).reduce(BigDecimal.ZERO, BigDecimal::add);
            ReportService.Summary yearly = reports.summary(year, null);
            ReportService.Summary monthly = reports.summary(year, LocalDate.now().getMonthValue());
            fb = new FinanceBlock(payments.totalForYear(year), pending, overdue, monthly.income(),
                    yearly.salaries().add(yearly.expenses()), yearly.series(), yearly.byMode());
        }
        return new Dashboard(students.countByStatus(Enums.StudentStatus.ACTIVE), teachers.countByActiveTrue(),
                all.size(), admin ? enquiries.count((r, q, cb) -> cb.equal(r.get("status"), Enums.EnquiryStatus.NEW)) : null,
                FinanceService.academicYearLabel(year), perClass, fb);
    }

    private static boolean has(Authentication a, String role) {
        return a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals(role));
    }
}
