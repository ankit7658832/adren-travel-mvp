package com.adren.travel;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architecture test — fails the build if any module reaches into another
 * module's {@code .internal} package, or if a cyclic dependency between
 * modules is introduced. This is the single most valuable test in a Spring
 * Modulith codebase: it turns "please don't couple modules together" from a
 * code-review nitpick into a CI failure.
 * <p>
 * Run as part of {@code ./gradlew test} (fast — no Spring context is
 * actually started, this only inspects bytecode/package structure).
 */
class ModularityTests {

    private static final ApplicationModules MODULES =
        ApplicationModules.of(AdrenTravelApplication.class);

    @Test
    void moduleBoundariesAreRespected() {
        MODULES.verify();
    }

    /**
     * TST-02 — {@code ApplicationModules.of(...)} scans the compiled
     * package structure itself, not a hand-maintained module list, so
     * {@code moduleBoundariesAreRespected()} above automatically starts
     * covering a module the moment it moves past a package-info-only stub —
     * no test file needs editing when that happens. This regression test
     * proves that mechanism actually covers every module with real content
     * as of today (every module except {@code compliance}, still a
     * package-info stub — PRD Section 17 is out of mock-phase scope
     * entirely, no {@code CMP-*}-equivalent mock story exists), so a
     * silent gap (a module quietly never getting scanned) would show up
     * here as a regression, not go unnoticed.
     */
    @Test
    void everyModuleWithRealContentIsAutoDiscoveredWithNoManualListToMaintain() {
        Set<String> discoveredModuleNames = StreamSupport.stream(MODULES.spliterator(), false)
            .map(module -> module.getIdentifier().toString())
            .collect(Collectors.toSet());

        Set<String> modulesWithRealContent = Set.of(
            "ads", "ai", "booking", "dashboard", "notification",
            "payments", "security", "shared", "supplier", "whitelabel");

        assertThat(discoveredModuleNames).containsAll(modulesWithRealContent);
    }

    @Test
    void writeModuleDocumentation() {
        // Regenerates docs/module-*.puml and docs/all-docs.adoc under
        // build/spring-modulith-docs — wire this into `doc/architecture/`
        // (see doc/README.md) as part of the release process so the module
        // diagram never drifts from the actual code.
        new Documenter(MODULES)
            .writeDocumentation()
            .writeIndividualModulesAsPlantUml();
    }
}
