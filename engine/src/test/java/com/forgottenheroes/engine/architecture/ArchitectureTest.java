package com.forgottenheroes.engine.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The engine's structural promises (ADR-3, ADR-4), checked on every build: no framework, no I/O, no
 * clock, no randomness but {@code GameRandom}. A rule here fails the build before a reviewer has to notice.
 */
@AnalyzeClasses(packages = "com.forgottenheroes.engine", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule noFrameworks = noClasses().should().dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "software.amazon..", "jakarta..", "javax..")
        .because("the engine is plain Java; Spring and AWS live in the backend (ADR-3)");

    @ArchTest
    static final ArchRule noIo = noClasses().should().dependOnClassesThat()
        .resideInAnyPackage("java.net..", "java.nio.file..", "java.sql..")
        .because("the engine reads nothing but its own classpath content");

    @ArchTest
    static final ArchRule noClocks = noClasses().should().dependOnClassesThat().resideInAPackage("java.time..")
        .because("time is not an input of the rules (ADR-4)");

    @ArchTest
    static final ArchRule noSystemTime = noClasses().should().callMethod(System.class, "currentTimeMillis")
        .orShould().callMethod(System.class, "nanoTime")
        .because("the same seed and actions must give the same events (ADR-4)");

    @ArchTest
    static final ArchRule noMathRandom = noClasses().should().callMethod(Math.class, "random")
        .because("GameRandom is the only dice (ADR-4)");

    @ArchTest
    static final ArchRule noJavaUtilRandom = noClasses().should().dependOnClassesThat()
        .haveFullyQualifiedName("java.util.Random")
        .orShould().dependOnClassesThat().haveFullyQualifiedName("java.security.SecureRandom")
        .because("GameRandom is the only dice; the run seed is drawn by the backend, not here (ADR-4)");

    @ArchTest
    static final ArchRule onlyGameRandomWrapsSplittableRandom = noClasses().that().doNotHaveSimpleName("GameRandom")
        .should().dependOnClassesThat().haveFullyQualifiedName("java.util.SplittableRandom")
        .because("every random draw goes through one seeded, forkable generator");

    @ArchTest
    static final ArchRule modelKnowsNoJson = noClasses().that().resideInAPackage("..engine.model..")
        .should().dependOnClassesThat().resideInAnyPackage("tools.jackson..", "com.fasterxml..")
        .because("the model is plain data; JSON lives in engine.json, engine.event and engine.content");

    @ArchTest
    static final ArchRule onlyMutatorsAndModelWriteGold = noClasses().that().resideOutsideOfPackages("..engine.mutator..", "..engine.model..")
        .should().callMethod("com.forgottenheroes.engine.model.RunState", "setGold", "int")
        .orShould().callMethod("com.forgottenheroes.engine.model.RunState", "setHearts", "int")
        .orShould().callMethod("com.forgottenheroes.engine.model.RunState", "setCrowns", "int")
        .because("gold, hearts and crowns change only through mutators, which emit the event");
}
