package com.forgottenheroes.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The backend's structural promises, checked on every build like the engine's. The one Step 31 adds is
 * the important one: the {@code X-Dev-User} shortcut can only ever be wired by the {@code local} profile.
 * {@code ProdSettingsTest} covers the other way the shortcut could sneak in, the settings file.
 */
@AnalyzeClasses(packages = "com.forgottenheroes.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule theLocalShortcutStaysLocal = noClasses().that().resideOutsideOfPackage("..security.local..")
        .should().dependOnClassesThat().resideInAPackage("..security.local..")
        .because("only the local profile may trust a header instead of a token (architecture §7)");

    @ArchTest
    static final ArchRule onlyTheDoormanReadsTokens = noClasses().that().resideOutsideOfPackage("..backend.security..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework.security..")
        .because("doors and services ask CurrentUser who is calling; how that was proven is the security package's business");

    @ArchTest
    static final ArchRule servicesNeverReadTheRequest = noClasses().that().resideInAnyPackage("..backend.service..", "..backend.repo..")
        .should().dependOnClassesThat().resideInAnyPackage("jakarta.servlet..", "org.springframework.web..")
        .because("who is calling arrives as a userId argument; a service that read the request could not be tested without HTTP");
}
