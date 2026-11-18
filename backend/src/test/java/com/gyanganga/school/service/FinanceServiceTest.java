package com.gyanganga.school.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyanganga.school.model.Enums.Frequency;
import com.gyanganga.school.model.FeeStructure;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FinanceServiceTest {

    private static FeeStructure head(long amount, Frequency f) {
        FeeStructure h = new FeeStructure();
        h.setAmount(BigDecimal.valueOf(amount));
        h.setFrequency(f);
        return h;
    }

    @ParameterizedTest
    @CsvSource({"2026-03-31,2025", "2026-04-01,2026", "2026-12-31,2026", "2027-01-01,2026", "2027-03-31,2026"})
    void academicYearStartsInApril(String date, int expected) {
        assertThat(FinanceService.academicYearOf(LocalDate.parse(date))).isEqualTo(expected);
    }

    @Test
    void academicYearLabelWrapsCentury() {
        assertThat(FinanceService.academicYearLabel(2026)).isEqualTo("2026-27");
        assertThat(FinanceService.academicYearLabel(2099)).isEqualTo("2099-00");
    }

    @Test
    void monthsElapsedCountsFromAprilAndIsCappedToTheYear() {
        assertThat(FinanceService.monthsElapsed(2026, LocalDate.parse("2026-03-31"))).isZero();
        assertThat(FinanceService.monthsElapsed(2026, LocalDate.parse("2026-04-01"))).isEqualTo(1);
        assertThat(FinanceService.monthsElapsed(2026, LocalDate.parse("2026-11-17"))).isEqualTo(8);
        assertThat(FinanceService.monthsElapsed(2026, LocalDate.parse("2027-03-31"))).isEqualTo(12);
        assertThat(FinanceService.monthsElapsed(2026, LocalDate.parse("2030-01-01"))).isEqualTo(12);
        assertThat(FinanceService.monthsElapsed(2026, LocalDate.parse("2025-06-01"))).isZero();
    }

    @Test
    void monthlyHeadIsTwelveTimesPerYearAndDueForMonthsElapsed() {
        var fees = FinanceService.feesFor(List.of(head(100, Frequency.MONTHLY)), 8);
        assertThat(fees.total()).isEqualByComparingTo("1200");
        assertThat(fees.dueToDate()).isEqualByComparingTo("800");
    }

    @Test
    void quarterlyHeadIsDueAtStartOfEachQuarter() {
        // months 1-3 -> 1 quarter, 4-6 -> 2, 7-9 -> 3, 10-12 -> 4
        assertThat(FinanceService.feesFor(List.of(head(300, Frequency.QUARTERLY)), 1).dueToDate()).isEqualByComparingTo("300");
        assertThat(FinanceService.feesFor(List.of(head(300, Frequency.QUARTERLY)), 3).dueToDate()).isEqualByComparingTo("300");
        assertThat(FinanceService.feesFor(List.of(head(300, Frequency.QUARTERLY)), 4).dueToDate()).isEqualByComparingTo("600");
        assertThat(FinanceService.feesFor(List.of(head(300, Frequency.QUARTERLY)), 8).dueToDate()).isEqualByComparingTo("900");
        assertThat(FinanceService.feesFor(List.of(head(300, Frequency.QUARTERLY)), 12).dueToDate()).isEqualByComparingTo("1200");
        assertThat(FinanceService.feesFor(List.of(head(300, Frequency.QUARTERLY)), 8).total()).isEqualByComparingTo("1200");
    }

    @Test
    void annualAndOneTimeHeadsAreDueOnceTheYearHasStarted() {
        var heads = List.of(head(1500, Frequency.ANNUAL), head(500, Frequency.ONCE));
        assertThat(FinanceService.feesFor(heads, 0).dueToDate()).isZero();
        assertThat(FinanceService.feesFor(heads, 1).dueToDate()).isEqualByComparingTo("2000");
        assertThat(FinanceService.feesFor(heads, 12).total()).isEqualByComparingTo("2000");
    }

    @Test
    void headsAreSummed() {
        var heads = List.of(head(100, Frequency.MONTHLY), head(300, Frequency.QUARTERLY), head(1500, Frequency.ANNUAL));
        var fees = FinanceService.feesFor(heads, 8);
        assertThat(fees.total()).isEqualByComparingTo("3900");     // 1200 + 1200 + 1500
        assertThat(fees.dueToDate()).isEqualByComparingTo("3200"); // 800 + 900 + 1500
    }

    @Test
    void noHeadsMeansNoFees() {
        var fees = FinanceService.feesFor(List.of(), 8);
        assertThat(fees.total()).isZero();
        assertThat(fees.dueToDate()).isZero();
    }
}
