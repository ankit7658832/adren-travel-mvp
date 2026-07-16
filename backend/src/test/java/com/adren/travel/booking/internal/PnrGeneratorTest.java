package com.adren.travel.booking.internal;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PnrGeneratorTest {

    private static final Pattern EXPECTED_FORMAT = Pattern.compile("^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{8}$");

    @Test
    void generateProducesAnEightCharacterUppercaseAlphanumericCodeBOK19() {
        String pnr = PnrGenerator.generate();

        assertThat(pnr).matches(EXPECTED_FORMAT);
    }

    @Test
    void generateExcludesVisuallyAmbiguousCharactersBOK19() {
        String pnr = PnrGenerator.generate();

        assertThat(pnr).doesNotContainAnyWhitespaces();
        assertThat(pnr.chars()).noneMatch(c -> "01ILO".indexOf(c) >= 0);
    }

    @Test
    void generateIsNotDeterministicAcrossManyCallsBOK19() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            generated.add(PnrGenerator.generate());
        }

        // 32^8 possibilities — 1000 draws colliding even once would be an
        // astronomically unlikely (and worth investigating) coincidence.
        assertThat(generated).hasSize(1000);
    }
}
