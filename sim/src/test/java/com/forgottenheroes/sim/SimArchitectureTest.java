package com.forgottenheroes.sim;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.forgottenheroes.engine.resolver.RuleViolation;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The simulator is a client of the engine, not a part of it: it may use the facade, the bots, the
 * content, the model, the actions, the events, the JSON module and the dice, plus the one exception
 * type the facade throws, and nothing from the resolvers or the mutators.
 */
@AnalyzeClasses(packages = "com.forgottenheroes.sim", importOptions = ImportOption.DoNotIncludeTests.class)
class SimArchitectureTest {

    @ArchTest
    static final ArchRule onlyThePublicFaceOfTheEngine = noClasses().should().dependOnClassesThat(
            resideInAnyPackage("..engine.resolver..", "..engine.mutator..").and(not(type(RuleViolation.class))))
        .because("the simulator drives the engine through GameSession and the bots only");
}
