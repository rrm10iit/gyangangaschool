package com.gyanganga.school.service;

import com.gyanganga.school.model.*;
import com.gyanganga.school.repo.Repos;
import com.gyanganga.school.web.FinanceControllers.PaymentView;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ReportService {
    public record SeriesPoint(String label, BigDecimal income, BigDecimal salaries, BigDecimal expenses) {}

    public record NamedAmount(String name, BigDecimal amount) {}

    public record Summary(int academicYear, String academicYearLabel, Integer month, String period,
                          BigDecimal income, BigDecimal salaries, BigDecimal expenses, BigDecimal net,
                          int paymentCount, List<SeriesPoint> series, List<NamedAmount> byMode,
                          List<NamedAmount> expenseByCategory) {}

    private static final Color NAVY = new Color(0x0B, 0x1F, 0x4B);
    private static final Color GOLD = new Color(0xF2, 0xB7, 0x05);

    private final Repos.PaymentRepo payments;
    private final Repos.SalaryRepo salaries;
    private final Repos.ExpenseRepo expenses;
    private final Repos.StudentRepo students;
    private final Repos.TeacherRepo teachers;
    private final Repos.ClassRepo classes;
    private final Repos.SettingRepo settings;

    public ReportService(Repos.PaymentRepo payments, Repos.SalaryRepo salaries, Repos.ExpenseRepo expenses,
                         Repos.StudentRepo students, Repos.TeacherRepo teachers, Repos.ClassRepo classes,
                         Repos.SettingRepo settings) {
        this.payments = payments;
        this.salaries = salaries;
        this.expenses = expenses;
        this.students = students;
        this.teachers = teachers;
        this.classes = classes;
        this.settings = settings;
    }

    public static String fileTag(int year, Integer month) {
        return FinanceService.academicYearLabel(year) + (month == null ? "" : "-m" + String.format("%02d", month));
    }

    private static LocalDate[] range(int year, Integer month) {
        if (month == null) return new LocalDate[] {LocalDate.of(year, 4, 1), LocalDate.of(year + 1, 3, 31)};
        if (month < 1 || month > 12) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid month");
        YearMonth ym = YearMonth.of(month >= 4 ? year : year + 1, month);
        return new LocalDate[] {ym.atDay(1), ym.atEndOfMonth()};
    }

    private static BigDecimal sum(Iterable<BigDecimal> values) {
        BigDecimal t = BigDecimal.ZERO;
        for (BigDecimal v : values) t = t.add(v);
        return t;
    }

    public Summary summary(int year, Integer month) {
        LocalDate[] r = range(year, month);
        List<FeePayment> pay = payments.findByPaymentDateBetweenOrderByPaymentDateAscIdAsc(r[0], r[1]);
        List<SalaryRecord> sal = salaries.findByStatusAndPaidDateBetweenOrderByPaidDateAscIdAsc(
                Enums.SalaryStatus.PAID, r[0], r[1]);
        List<Expense> exp = expenses.findByExpenseDateBetweenOrderByExpenseDateAscIdAsc(r[0], r[1]);

        BigDecimal income = sum(pay.stream().map(FeePayment::getAmount).toList());
        BigDecimal salTotal = sum(sal.stream().map(SalaryRecord::getNetAmount).toList());
        BigDecimal expTotal = sum(exp.stream().map(Expense::getAmount).toList());

        // time series: one point per month for a year, one per day for a month
        Map<String, BigDecimal[]> buckets = new LinkedHashMap<>();
        if (month == null) {
            for (int i = 0; i < 12; i++) {
                YearMonth ym = YearMonth.of(year, 4).plusMonths(i);
                buckets.put(key(ym), zeros());
            }
        } else {
            for (LocalDate d = r[0]; !d.isAfter(r[1]); d = d.plusDays(1)) buckets.put(dayKey(d), zeros());
        }
        pay.forEach(p -> add(buckets, month == null ? key(YearMonth.from(p.getPaymentDate())) : dayKey(p.getPaymentDate()), 0, p.getAmount()));
        sal.forEach(s -> add(buckets, month == null ? key(YearMonth.from(s.getPaidDate())) : dayKey(s.getPaidDate()), 1, s.getNetAmount()));
        exp.forEach(e -> add(buckets, month == null ? key(YearMonth.from(e.getExpenseDate())) : dayKey(e.getExpenseDate()), 2, e.getAmount()));
        List<SeriesPoint> series = new ArrayList<>();
        buckets.forEach((k, v) -> series.add(new SeriesPoint(k, v[0], v[1], v[2])));

        Map<String, BigDecimal> byMode = new LinkedHashMap<>();
        pay.forEach(p -> byMode.merge(p.getMode().name(), p.getAmount(), BigDecimal::add));
        Map<String, BigDecimal> byCat = new LinkedHashMap<>();
        exp.forEach(e -> byCat.merge(e.getCategory(), e.getAmount(), BigDecimal::add));

        String period = month == null ? "Academic year " + FinanceService.academicYearLabel(year)
                : YearMonth.from(r[0]).getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + r[0].getYear();
        return new Summary(year, FinanceService.academicYearLabel(year), month, period, income, salTotal, expTotal,
                income.subtract(salTotal).subtract(expTotal), pay.size(), series, named(byMode), named(byCat));
    }

    private static String key(YearMonth ym) {
        return ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + ym.getYear();
    }

    private static String dayKey(LocalDate d) {
        return String.valueOf(d.getDayOfMonth());
    }

    private static BigDecimal[] zeros() {
        return new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
    }

    private static void add(Map<String, BigDecimal[]> m, String key, int idx, BigDecimal v) {
        BigDecimal[] a = m.get(key);
        if (a != null) a[idx] = a[idx].add(v);
    }

    private static List<NamedAmount> named(Map<String, BigDecimal> m) {
        return m.entrySet().stream().map(e -> new NamedAmount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(NamedAmount::amount).reversed()).toList();
    }

    // ------------------------------------------------------------------ Excel

    public byte[] excel(int year, Integer month) {
        LocalDate[] r = range(year, month);
        Summary s = summary(year, month);
        Map<Long, Student> st = new HashMap<>();
        students.findAll().forEach(x -> st.put(x.getId(), x));
        Map<Long, String> cn = new HashMap<>();
        classes.findAll().forEach(c -> cn.put(c.getId(), c.getName()));
        Map<Long, Teacher> tc = new HashMap<>();
        teachers.findAll().forEach(t -> tc.put(t.getId(), t));

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle head = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font hf = wb.createFont();
            hf.setBold(true);
            hf.setColor(IndexedColors.WHITE.getIndex());
            head.setFont(hf);
            head.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            CellStyle bold = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font bf = wb.createFont();
            bf.setBold(true);
            bold.setFont(bf);

            Sheet sum = wb.createSheet("Summary");
            int i = 0;
            row(sum, i++, bold, "Gyan Ganga School - Finance report");
            row(sum, i++, null, s.period());
            i++;
            moneyRow(sum, i++, money, "Fee collection", s.income());
            moneyRow(sum, i++, money, "Salaries paid", s.salaries());
            moneyRow(sum, i++, money, "Other expenses", s.expenses());
            moneyRow(sum, i++, money, "Net surplus / (deficit)", s.net());
            i++;
            row(sum, i++, bold, month == null ? "Month" : "Day", "Fees", "Salaries", "Expenses");
            for (SeriesPoint p : s.series()) {
                Row rw = sum.createRow(i++);
                rw.createCell(0).setCellValue(p.label());
                num(rw, 1, p.income(), money);
                num(rw, 2, p.salaries(), money);
                num(rw, 3, p.expenses(), money);
            }
            sum.autoSizeColumn(0);
            for (int c = 1; c < 4; c++) sum.setColumnWidth(c, 4500);

            Sheet fees = wb.createSheet("Fee collection");
            header(fees, head, "Receipt", "Date", "Student", "Admission no", "Class", "Amount", "Mode", "For period", "Reference", "Collected by");
            int fr = 1;
            for (FeePayment p : payments.findByPaymentDateBetweenOrderByPaymentDateAscIdAsc(r[0], r[1])) {
                Student x = st.get(p.getStudentId());
                Row rw = fees.createRow(fr++);
                rw.createCell(0).setCellValue(p.getReceiptNo());
                rw.createCell(1).setCellValue(p.getPaymentDate().toString());
                rw.createCell(2).setCellValue(x == null ? "" : x.getFullName());
                rw.createCell(3).setCellValue(x == null ? "" : x.getAdmissionNo());
                rw.createCell(4).setCellValue(x == null ? "" : cn.getOrDefault(x.getClassId(), ""));
                num(rw, 5, p.getAmount(), money);
                rw.createCell(6).setCellValue(p.getMode().name());
                rw.createCell(7).setCellValue(nz(p.getForPeriod()));
                rw.createCell(8).setCellValue(nz(p.getReference()));
                rw.createCell(9).setCellValue(nz(p.getCollectedBy()));
            }
            widths(fees, 10);

            Sheet sal = wb.createSheet("Salaries");
            header(sal, head, "Paid on", "Month", "Teacher", "Employee no", "Base", "Allowances", "Deductions", "Net paid", "Mode");
            int sr = 1;
            for (SalaryRecord x : salaries.findByStatusAndPaidDateBetweenOrderByPaidDateAscIdAsc(Enums.SalaryStatus.PAID, r[0], r[1])) {
                Teacher t = tc.get(x.getTeacherId());
                Row rw = sal.createRow(sr++);
                rw.createCell(0).setCellValue(x.getPaidDate().toString());
                rw.createCell(1).setCellValue(x.getMonth());
                rw.createCell(2).setCellValue(t == null ? "" : t.getFullName());
                rw.createCell(3).setCellValue(t == null ? "" : t.getEmployeeNo());
                num(rw, 4, x.getBaseAmount(), money);
                num(rw, 5, x.getAllowances(), money);
                num(rw, 6, x.getDeductions(), money);
                num(rw, 7, x.getNetAmount(), money);
                rw.createCell(8).setCellValue(x.getMode() == null ? "" : x.getMode().name());
            }
            widths(sal, 9);

            Sheet ex = wb.createSheet("Expenses");
            header(ex, head, "Date", "Category", "Description", "Paid to", "Amount", "Mode");
            int er = 1;
            for (Expense x : expenses.findByExpenseDateBetweenOrderByExpenseDateAscIdAsc(r[0], r[1])) {
                Row rw = ex.createRow(er++);
                rw.createCell(0).setCellValue(x.getExpenseDate().toString());
                rw.createCell(1).setCellValue(x.getCategory());
                rw.createCell(2).setCellValue(x.getDescription());
                rw.createCell(3).setCellValue(nz(x.getPaidTo()));
                num(rw, 4, x.getAmount(), money);
                rw.createCell(5).setCellValue(x.getMode() == null ? "" : x.getMode().name());
            }
            widths(ex, 6);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create the Excel file");
        }
    }

    private static void row(Sheet sh, int idx, CellStyle style, String... vals) {
        Row r = sh.createRow(idx);
        for (int c = 0; c < vals.length; c++) {
            Cell cell = r.createCell(c);
            cell.setCellValue(vals[c]);
            if (style != null) cell.setCellStyle(style);
        }
    }

    private static void moneyRow(Sheet sh, int idx, CellStyle money, String label, BigDecimal v) {
        Row r = sh.createRow(idx);
        r.createCell(0).setCellValue(label);
        num(r, 1, v, money);
    }

    private static void num(Row r, int c, BigDecimal v, CellStyle style) {
        Cell cell = r.createCell(c);
        cell.setCellValue(v == null ? 0 : v.doubleValue());
        cell.setCellStyle(style);
    }

    private static void header(Sheet sh, CellStyle style, String... names) {
        Row r = sh.createRow(0);
        for (int c = 0; c < names.length; c++) {
            Cell cell = r.createCell(c);
            cell.setCellValue(names[c]);
            cell.setCellStyle(style);
        }
        sh.createFreezePane(0, 1);
    }

    private static void widths(Sheet sh, int cols) {
        for (int c = 0; c < cols; c++) sh.setColumnWidth(c, c == 2 ? 9000 : 4800);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    // ------------------------------------------------------------------ PDF

    public byte[] pdf(int year, Integer month) {
        Summary s = summary(year, month);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NAVY);
            Font h2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, NAVY);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);
            doc.add(new Paragraph(schoolName() + " - Finance report", h1));
            doc.add(new Paragraph(s.period() + "    Generated " + LocalDate.now(), body));
            doc.add(new Paragraph(" "));

            PdfPTable t = table(2, 60);
            kv(t, "Fee collection", money(s.income()), body);
            kv(t, "Salaries paid", money(s.salaries()), body);
            kv(t, "Other expenses", money(s.expenses()), body);
            kv(t, "Net surplus / (deficit)", money(s.net()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
            doc.add(t);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(month == null ? "Month by month" : "Day by day", h2));
            PdfPTable series = table(4, 100);
            headerRow(series, body, month == null ? "Month" : "Day", "Fees", "Salaries", "Expenses");
            for (SeriesPoint p : s.series()) {
                if (month != null && p.income().signum() == 0 && p.salaries().signum() == 0 && p.expenses().signum() == 0) continue;
                cell(series, p.label(), body, Element.ALIGN_LEFT);
                cell(series, money(p.income()), body, Element.ALIGN_RIGHT);
                cell(series, money(p.salaries()), body, Element.ALIGN_RIGHT);
                cell(series, money(p.expenses()), body, Element.ALIGN_RIGHT);
            }
            doc.add(series);

            if (!s.byMode().isEmpty()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Fee collection by payment mode", h2));
                PdfPTable m = table(2, 60);
                s.byMode().forEach(n -> kv(m, n.name(), money(n.amount()), body));
                doc.add(m);
            }
            if (!s.expenseByCategory().isEmpty()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Expenses by category", h2));
                PdfPTable m = table(2, 60);
                s.expenseByCategory().forEach(n -> kv(m, n.name(), money(n.amount()), body));
                doc.add(m);
            }
            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create the PDF");
        }
    }

    // ------------------------------------------------------------------ Receipt

    public byte[] receiptPdf(PaymentView p) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A5.rotate(), 30, 30, 28, 28);
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NAVY);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

            Paragraph name = new Paragraph(schoolName(), title);
            name.setAlignment(Element.ALIGN_CENTER);
            doc.add(name);
            String address = setting("school.address");
            String phone = setting("school.phone");
            Paragraph addr = new Paragraph(address + (phone.isBlank() ? "" : "  |  Phone: " + phone), small);
            addr.setAlignment(Element.ALIGN_CENTER);
            doc.add(addr);
            Paragraph rt = new Paragraph("FEE RECEIPT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, GOLD.darker()));
            rt.setAlignment(Element.ALIGN_CENTER);
            rt.setSpacingBefore(8);
            rt.setSpacingAfter(8);
            doc.add(rt);

            PdfPTable t = new PdfPTable(new float[] {1.2f, 2.8f, 1.2f, 2.0f});
            t.setWidthPercentage(100);
            pair(t, "Receipt no", p.receiptNo(), "Date", p.paymentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)), bold, body);
            pair(t, "Student", p.studentName(), "Admission no", p.admissionNo(), bold, body);
            pair(t, "Class", p.className() + (p.section() == null || p.section().isBlank() ? "" : " - " + p.section()),
                    "Academic year", FinanceService.academicYearLabel(p.academicYear()), bold, body);
            pair(t, "For", p.forPeriod() == null ? "Fees" : p.forPeriod(), "Mode",
                    p.mode().name() + (p.reference() == null ? "" : " (" + p.reference() + ")"), bold, body);
            doc.add(t);

            PdfPTable amt = new PdfPTable(1);
            amt.setWidthPercentage(100);
            amt.setSpacingBefore(10);
            PdfPCell c = new PdfPCell(new Phrase("Amount received: Rs. " + money(p.amount()) + "\n"
                    + amountInWords(p.amount()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, NAVY)));
            c.setPadding(8);
            c.setBorderColor(GOLD);
            c.setBorderWidth(1.5f);
            amt.addCell(c);
            doc.add(amt);

            if (p.remarks() != null) {
                Paragraph rm = new Paragraph("Remarks: " + p.remarks(), small);
                rm.setSpacingBefore(6);
                doc.add(rm);
            }
            Paragraph sign = new Paragraph("\n\nReceived by: " + (p.collectedBy() == null ? "" : p.collectedBy())
                    + "                                                    Authorised signature", small);
            doc.add(sign);
            Paragraph foot = new Paragraph("This is a computer generated receipt.", small);
            foot.setAlignment(Element.ALIGN_CENTER);
            foot.setSpacingBefore(6);
            doc.add(foot);
            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create the receipt");
        }
    }

    private String setting(String key) {
        return settings.findBySettingKey(key).map(SiteSetting::getValueEn).orElse("");
    }

    private String schoolName() {
        String n = setting("school.name");
        return n.isBlank() ? "Gyan Ganga School" : n;
    }

    private static PdfPTable table(int cols, float widthPct) {
        PdfPTable t = new PdfPTable(cols);
        t.setWidthPercentage(widthPct);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        return t;
    }

    private static void headerRow(PdfPTable t, Font f, String... names) {
        Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        for (String n : names) {
            PdfPCell c = new PdfPCell(new Phrase(n, white));
            c.setBackgroundColor(NAVY);
            c.setPadding(5);
            t.addCell(c);
        }
    }

    private static void cell(PdfPTable t, String text, Font f, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setPadding(4);
        c.setHorizontalAlignment(align);
        c.setBorderColor(Color.LIGHT_GRAY);
        t.addCell(c);
    }

    private static void kv(PdfPTable t, String k, String v, Font f) {
        cell(t, k, f, Element.ALIGN_LEFT);
        cell(t, v, f, Element.ALIGN_RIGHT);
    }

    private static void pair(PdfPTable t, String k1, String v1, String k2, String v2, Font bold, Font body) {
        for (String[] kv : new String[][] {{k1, "b"}, {v1, "n"}, {k2, "b"}, {v2, "n"}}) {
            PdfPCell c = new PdfPCell(new Phrase(kv[0] == null ? "" : kv[0], kv[1].equals("b") ? bold : body));
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(Color.LIGHT_GRAY);
            c.setPadding(5);
            t.addCell(c);
        }
    }

    static String money(BigDecimal v) {
        BigDecimal x = v.setScale(2, RoundingMode.HALF_UP);
        return String.format(Locale.ENGLISH, "%,.2f", x);
    }

    // ---- amount in words (Indian numbering) ----
    private static final String[] ONES = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

    static String amountInWords(BigDecimal amount) {
        BigDecimal a = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = a.longValue();
        int paise = a.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        String w = "Rupees " + (rupees == 0 ? "Zero" : words(rupees));
        if (paise > 0) w += " and " + words(paise) + " Paise";
        return w + " Only";
    }

    private static String words(long n) {
        if (n < 20) return ONES[(int) n];
        if (n < 100) return TENS[(int) (n / 10)] + (n % 10 == 0 ? "" : " " + ONES[(int) (n % 10)]);
        if (n < 1000) return ONES[(int) (n / 100)] + " Hundred" + (n % 100 == 0 ? "" : " " + words(n % 100));
        if (n < 100000) return words(n / 1000) + " Thousand" + (n % 1000 == 0 ? "" : " " + words(n % 1000));
        if (n < 10000000) return words(n / 100000) + " Lakh" + (n % 100000 == 0 ? "" : " " + words(n % 100000));
        return words(n / 10000000) + " Crore" + (n % 10000000 == 0 ? "" : " " + words(n % 10000000));
    }
}
