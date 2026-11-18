package com.gyanganga.school.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReportServiceTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0|Rupees Zero Only",
            "1|Rupees One Only",
            "19|Rupees Nineteen Only",
            "20|Rupees Twenty Only",
            "99|Rupees Ninety Nine Only",
            "100|Rupees One Hundred Only",
            "1234.50|Rupees One Thousand Two Hundred Thirty Four and Fifty Paise Only",
            "100000|Rupees One Lakh Only",
            "150000|Rupees One Lakh Fifty Thousand Only",
            "10000000|Rupees One Crore Only",
            "0.05|Rupees Zero and Five Paise Only",
    })
    void amountInWordsUsesIndianNumbering(String amount, String expected) {
        assertThat(ReportService.amountInWords(new BigDecimal(amount))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"0|0.00", "999.5|999.50", "1234567.891|1,234,567.89"})
    void moneyIsFormattedWithTwoDecimals(String in, String expected) {
        assertThat(ReportService.money(new BigDecimal(in))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"2026,,2026-27", "2026,4,2026-27-m04", "2026,1,2026-27-m01"})
    void fileTagIdentifiesThePeriod(int year, Integer month, String expected) {
        assertThat(ReportService.fileTag(year, month)).isEqualTo(expected);
    }
}
