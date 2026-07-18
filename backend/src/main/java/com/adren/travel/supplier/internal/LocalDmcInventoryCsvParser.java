package com.adren.travel.supplier.internal;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.ProductCategory;
import com.adren.travel.supplier.LocalDmcInventoryItemCommand;
import com.adren.travel.supplier.LocalDmcInventoryRowError;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses + row-validates a Local DMC bulk-inventory CSV upload (PRD
 * §10.2.8, DMC-03) — required header row: {@code productName, category,
 * netRate, netRateCurrency, cancellationPolicyText, availableFrom,
 * availableTo} (dates ISO {@code yyyy-MM-dd}). Every row is checked for
 * EVERY field error before deciding pass/fail (not stopping at the first
 * bad field), matching the story's own "row-level, field-level errors"
 * AC — a row with 3 missing fields reports all 3, not just the first.
 */
@Component
class LocalDmcInventoryCsvParser {

    record ParseResult(List<LocalDmcInventoryItemCommand> validRows, List<LocalDmcInventoryRowError> errors) {
    }

    ParseResult parse(String csvContent) {
        List<LocalDmcInventoryItemCommand> validRows = new ArrayList<>();
        List<LocalDmcInventoryRowError> errors = new ArrayList<>();

        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build();
        try (CSVParser parser = CSVParser.parse(new StringReader(csvContent), format)) {
            for (CSVRecord record : parser) {
                List<String> fieldErrors = new ArrayList<>();

                String productName = requiredField(record, "productName", fieldErrors);
                ProductCategory category = requiredEnum(record, "category", ProductCategory.class, fieldErrors);
                BigDecimal netRate = requiredDecimal(record, "netRate", fieldErrors);
                CurrencyCode netRateCurrency = requiredEnum(record, "netRateCurrency", CurrencyCode.class, fieldErrors);
                String cancellationPolicyText = requiredField(record, "cancellationPolicyText", fieldErrors);
                LocalDate availableFrom = requiredDate(record, "availableFrom", fieldErrors);
                LocalDate availableTo = requiredDate(record, "availableTo", fieldErrors);

                if (fieldErrors.isEmpty()) {
                    validRows.add(new LocalDmcInventoryItemCommand(productName, category, netRate, netRateCurrency,
                        cancellationPolicyText, availableFrom, availableTo));
                } else {
                    // getRecordNumber() is 1-based and already excludes the
                    // header row (setSkipHeaderRecord), matching the AC's
                    // own "row-level" numbering a Consultant would expect
                    // when counting data rows in their spreadsheet.
                    errors.add(new LocalDmcInventoryRowError((int) record.getRecordNumber(), fieldErrors));
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Malformed CSV content: " + e.getMessage());
        } catch (java.io.UncheckedIOException e) {
            // commons-csv's iterator wraps a genuinely malformed row (e.g.
            // an unterminated quoted field — EOF reached mid-token) as this
            // unchecked type when lazily reading the next record, not a
            // plain IOException the try-with-resources catch above sees.
            throw new IllegalArgumentException("Malformed CSV content: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // commons-csv throws IllegalArgumentException (not IOException)
            // for a missing/malformed header line.
            throw new IllegalArgumentException("Malformed CSV header: " + e.getMessage());
        }
        return new ParseResult(validRows, errors);
    }

    private static String requiredField(CSVRecord record, String column, List<String> fieldErrors) {
        String value = valueOrNull(record, column);
        if (value == null || value.isBlank()) {
            fieldErrors.add(column + " is required");
            return null;
        }
        return value;
    }

    private static <E extends Enum<E>> E requiredEnum(CSVRecord record, String column, Class<E> type, List<String> fieldErrors) {
        String value = requiredField(record, column, fieldErrors);
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            fieldErrors.add(column + " is not a recognized value: " + value);
            return null;
        }
    }

    private static BigDecimal requiredDecimal(CSVRecord record, String column, List<String> fieldErrors) {
        String value = requiredField(record, column, fieldErrors);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            fieldErrors.add(column + " is not a valid number: " + value);
            return null;
        }
    }

    private static LocalDate requiredDate(CSVRecord record, String column, List<String> fieldErrors) {
        String value = requiredField(record, column, fieldErrors);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            fieldErrors.add(column + " is not a valid date (expected yyyy-MM-dd): " + value);
            return null;
        }
    }

    private static String valueOrNull(CSVRecord record, String column) {
        if (!record.isMapped(column) || !record.isSet(column)) {
            return null;
        }
        return record.get(column);
    }
}
