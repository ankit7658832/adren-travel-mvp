package com.adren.travel;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

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
