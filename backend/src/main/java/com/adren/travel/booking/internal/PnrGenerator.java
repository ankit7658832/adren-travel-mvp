package com.adren.travel.booking.internal;

import java.security.SecureRandom;

/**
 * Generates Adren's own PNR-searchable reference (PRD §20.8, BOK-19) —
 * an 8-character uppercase alphanumeric code, the same shape airline PNRs
 * use, but distinct from any actual airline PNR or supplier booking
 * reference (this is Adren-internal, generated independent of any supplier
 * call). Excludes visually-ambiguous characters (0/O, 1/I/L) so a support
 * agent reading one back over the phone doesn't misdial it.
 */
final class PnrGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PnrGenerator() {
    }

    static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
