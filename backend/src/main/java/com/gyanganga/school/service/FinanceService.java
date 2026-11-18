package com.gyanganga.school.service;

import com.gyanganga.school.model.*;
import com.gyanganga.school.repo.Repos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fee calculations. The academic year runs April to March and is identified by its starting year. */
@Service
@Transactional(readOnly = true)
public class FinanceService {

    public record DueRow(Long studentId, String admissionNo, String fullName, Long classId, String className,
                         String section, String guardianPhone, BigDecimal totalFee, BigDecimal discount,
                         BigDecimal paid, BigDecimal pending, BigDecimal dueToDate, BigDecimal overdue) {}

    public record Ledger(Student student, String className, int academicYear, BigDecimal totalFee,
                         BigDecimal discount, BigDecimal paid, BigDecimal pending, BigDecimal overdue,
                         List<FeeStructure> heads, List<FeePayment> payments) {}

    private final Repos.StudentRepo students;
    private final Repos.ClassRepo classes;
    private final Repos.FeeStructureRepo structures;
    private final Repos.PaymentRepo payments;

    public FinanceService(Repos.StudentRepo students, Repos.ClassRepo classes, Repos.FeeStructureRepo structures,
                          Repos.PaymentRepo payments) {
        this.students = students;
        this.classes = classes;
        this.structures = structures;
        this.payments = payments;
    }

    public static int academicYearOf(LocalDate d) {
        return d.getMonthValue() >= 4 ? d.getYear() : d.getYear() - 1;
    }

    public static String academicYearLabel(int year) {
        return year + "-" + String.format("%02d", (year + 1) % 100);
    }

    /** Number of months of the academic year that have started as of today (0 to 12). */
    static int monthsElapsed(int academicYear, LocalDate today) {
        int m = (today.getYear() - academicYear) * 12 + today.getMonthValue() - 4 + 1;
        return Math.max(0, Math.min(12, m));
    }

    private record ClassFees(BigDecimal total, BigDecimal dueToDate) {}

    private static ClassFees feesFor(List<FeeStructure> heads, int months) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal due = BigDecimal.ZERO;
        int quarters = (months + 2) / 3;
        for (FeeStructure h : heads) {
            BigDecimal a = h.getAmount();
            switch (h.getFrequency()) {
                case MONTHLY -> {
                    total = total.add(a.multiply(BigDecimal.valueOf(12)));
                    due = due.add(a.multiply(BigDecimal.valueOf(months)));
                }
                case QUARTERLY -> {
                    total = total.add(a.multiply(BigDecimal.valueOf(4)));
                    due = due.add(a.multiply(BigDecimal.valueOf(quarters)));
                }
                default -> {
                    total = total.add(a);
                    if (months > 0) due = due.add(a);
                }
            }
        }
        return new ClassFees(total, due);
    }

    private static BigDecimal nonNegative(BigDecimal v) {
        return v.signum() < 0 ? BigDecimal.ZERO : v;
    }

    /** Fee position of every active student (optionally for one class) for an academic year. */
    public List<DueRow> dues(Long classId, int year, String q) {
        int months = monthsElapsed(year, LocalDate.now());
        Map<Long, SchoolClass> classMap = classes.findAll().stream()
                .collect(Collectors.toMap(SchoolClass::getId, Function.identity()));
        Map<Long, ClassFees> feeMap = new HashMap<>();
        structures.findAll().stream().collect(Collectors.groupingBy(FeeStructure::getClassId))
                .forEach((cid, heads) -> feeMap.put(cid, feesFor(heads, months)));
        Map<Long, BigDecimal> paidMap = new HashMap<>();
        for (Object[] r : payments.sumByStudentForYear(year)) paidMap.put((Long) r[0], (BigDecimal) r[1]);

        String needle = q == null ? "" : q.trim().toLowerCase();
        List<DueRow> rows = new ArrayList<>();
        for (Student s : students.findByStatus(Enums.StudentStatus.ACTIVE)) {
            if (classId != null && !classId.equals(s.getClassId())) continue;
            if (!needle.isEmpty() && !(s.getFullName().toLowerCase().contains(needle)
                    || s.getAdmissionNo().toLowerCase().contains(needle))) continue;
            ClassFees cf = feeMap.getOrDefault(s.getClassId(), new ClassFees(BigDecimal.ZERO, BigDecimal.ZERO));
            BigDecimal discount = s.getFeeDiscount() == null ? BigDecimal.ZERO : s.getFeeDiscount();
            BigDecimal total = nonNegative(cf.total().subtract(discount));
            BigDecimal due = nonNegative(cf.dueToDate().subtract(discount));
            BigDecimal paid = paidMap.getOrDefault(s.getId(), BigDecimal.ZERO);
            SchoolClass c = classMap.get(s.getClassId());
            rows.add(new DueRow(s.getId(), s.getAdmissionNo(), s.getFullName(), s.getClassId(),
                    c == null ? "" : c.getName(), s.getSection(), s.getGuardianPhone(), total, discount, paid,
                    nonNegative(total.subtract(paid)), due, nonNegative(due.subtract(paid))));
        }
        rows.sort(java.util.Comparator.comparing(DueRow::overdue).reversed().thenComparing(DueRow::fullName));
        return rows;
    }

    public Ledger ledger(Student s, int year) {
        List<FeeStructure> heads = structures.findByClassId(s.getClassId());
        int months = monthsElapsed(year, LocalDate.now());
        ClassFees cf = feesFor(heads, months);
        BigDecimal discount = s.getFeeDiscount() == null ? BigDecimal.ZERO : s.getFeeDiscount();
        BigDecimal total = nonNegative(cf.total().subtract(discount));
        BigDecimal due = nonNegative(cf.dueToDate().subtract(discount));
        BigDecimal paid = payments.totalForStudent(s.getId(), year);
        String className = classes.findById(s.getClassId()).map(SchoolClass::getName).orElse("");
        List<FeePayment> list = payments.findByStudentIdOrderByPaymentDateDescIdDesc(s.getId()).stream()
                .filter(p -> p.getAcademicYear() == year).toList();
        return new Ledger(s, className, year, total, discount, paid, nonNegative(total.subtract(paid)),
                nonNegative(due.subtract(paid)), heads, list);
    }

    public BigDecimal totalPending(int year) {
        return dues(null, year, null).stream().map(DueRow::pending).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
