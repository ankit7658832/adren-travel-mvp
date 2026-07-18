package com.adren.travel.supplier.internal;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.ProductCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDmcInventoryCsvParserTest {

    private static final String HEADER = "productName,category,netRate,netRateCurrency,cancellationPolicyText,availableFrom,availableTo";

    private final LocalDmcInventoryCsvParser parser = new LocalDmcInventoryCsvParser();

    @Test
    void aFullyValidCsvProducesZeroErrorsAndOneRowPerLineDMC03() {
        String csv = HEADER + "\n"
            + "City Tour,ACTIVITY,2000,INR,\"Free cancellation, up to 24 hours before\",2026-08-01,2026-12-31\n"
            + "Airport Transfer,TRANSFER,1500,INR,No refunds,2026-08-01,2026-12-31\n";

        LocalDmcInventoryCsvParser.ParseResult result = parser.parse(csv);

        assertThat(result.errors()).isEmpty();
        assertThat(result.validRows()).hasSize(2);
        assertThat(result.validRows().get(0).productName()).isEqualTo("City Tour");
        assertThat(result.validRows().get(0).category()).isEqualTo(ProductCategory.ACTIVITY);
        assertThat(result.validRows().get(0).netRate()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(result.validRows().get(0).netRateCurrency()).isEqualTo(CurrencyCode.INR);
        // A comma embedded in a quoted field is parsed as ONE field, not
        // split into two — the whole reason this uses commons-csv rather
        // than a hand-rolled comma-split.
        assertThat(result.validRows().get(0).cancellationPolicyText())
            .isEqualTo("Free cancellation, up to 24 hours before");
        assertThat(result.validRows().get(0).availableFrom()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(result.validRows().get(0).availableTo()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void aRowMissingARequiredFieldRejectsTheWholeUploadWithFieldLevelErrorsDMC03() {
        String csv = HEADER + "\n"
            + "City Tour,,2000,INR,Free cancellation,2026-08-01,2026-12-31\n";

        LocalDmcInventoryCsvParser.ParseResult result = parser.parse(csv);

        assertThat(result.validRows()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).rowNumber()).isEqualTo(1);
        assertThat(result.errors().get(0).fieldErrors()).contains("category is required");
    }

    @Test
    void aRowWithMultipleBadFieldsReportsEveryFieldErrorNotJustTheFirstDMC03() {
        String csv = HEADER + "\n"
            + ",BOGUS_CATEGORY,not-a-number,BOGUS_CURRENCY,,not-a-date,2026-12-31\n";

        LocalDmcInventoryCsvParser.ParseResult result = parser.parse(csv);

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).fieldErrors()).hasSize(6)
            .anyMatch(e -> e.contains("productName"))
            .anyMatch(e -> e.contains("category"))
            .anyMatch(e -> e.contains("netRate"))
            .anyMatch(e -> e.contains("netRateCurrency"))
            .anyMatch(e -> e.contains("cancellationPolicyText"))
            .anyMatch(e -> e.contains("availableFrom"));
    }

    @Test
    void oneBadRowAmongOtherwiseValidRowsStillRejectsEveryRowNotAPartialImportDMC03() {
        String csv = HEADER + "\n"
            + "City Tour,ACTIVITY,2000,INR,Free cancellation,2026-08-01,2026-12-31\n"
            + "Bad Row,ACTIVITY,not-a-number,INR,Free cancellation,2026-08-01,2026-12-31\n";

        LocalDmcInventoryCsvParser.ParseResult result = parser.parse(csv);

        // The valid row is parsed as a candidate, but the caller
        // (LocalDmcService) is the one that enforces all-or-nothing by
        // never persisting anything when errors() is non-empty — this
        // parser's own job is just accurate row-level reporting.
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).rowNumber()).isEqualTo(2);
    }

    @Test
    void rowNumbersAreOneBasedAndExcludeTheHeaderDMC03() {
        String csv = HEADER + "\n"
            + "Valid One,ACTIVITY,2000,INR,Free cancellation,2026-08-01,2026-12-31\n"
            + "Valid Two,ACTIVITY,2000,INR,Free cancellation,2026-08-01,2026-12-31\n"
            + "Bad Three,,2000,INR,Free cancellation,2026-08-01,2026-12-31\n";

        LocalDmcInventoryCsvParser.ParseResult result = parser.parse(csv);

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).rowNumber()).isEqualTo(3);
    }

    @Test
    void genuinelyMalformedCsvContentThrowsRatherThanSilentlyProducingNoRows() {
        // An unterminated quote — the parser can never resolve the quoted
        // field's end before EOF, a real malformed-input case (unlike a
        // data row with fewer columns than the header, which commons-csv
        // tolerates by leaving the missing fields unset).
        assertThatThrownBy(() -> parser.parse(HEADER + "\n\"City Tour,ACTIVITY,2000,INR,unterminated"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
