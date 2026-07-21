package com.adren.travel.ai;

import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TST-09 — shared assertion enforcing PRD §11.2 principle 5 / §24.3's
 * 100%-logged, no-sampling AI governance requirement (AI-07) from the test
 * suite itself, not just a documented invariant reviewers have to remember
 * to check. {@code AiSuggestionAuditLogRepository}/{@code
 * AiSuggestionAuditLog} are {@code ai.internal}-package-private, so this
 * counts rows via a raw JDBC query on the table name rather than the
 * repository — the same established convention every other cross-boundary
 * test assertion in this codebase already uses for a table with no public
 * query API shaped for this (e.g. {@code FullVerticalSliceEndToEndIT}'s
 * direct voucher-row query).
 * <p>
 * Usage: capture {@link #currentAuditLogRowCount} before making N AI
 * calls, then assert the delta via {@link #assertExactlyNNewAuditLogRows}
 * — this catches a dropped/sampled write (fewer new rows than calls) AND
 * an accidental double-write (more new rows than calls) equally, unlike a
 * test that only checks "at least one row exists."
 */
public final class AiAuditCompletenessAssertions {

    private AiAuditCompletenessAssertions() {
    }

    public static long currentAuditLogRowCount(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_suggestion_audit_log", Long.class);
    }

    public static void assertExactlyNNewAuditLogRows(JdbcTemplate jdbcTemplate, long countBefore, long expectedNewRows) {
        long countAfter = currentAuditLogRowCount(jdbcTemplate);
        assertThat(countAfter - countBefore)
            .as("PRD S11.2/S24.3: every AI call attempt must produce exactly one audit-log row - no sampling, no dropped writes")
            .isEqualTo(expectedNewRows);
    }
}
