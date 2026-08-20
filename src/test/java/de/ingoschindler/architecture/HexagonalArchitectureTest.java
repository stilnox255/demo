package de.ingoschindler.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Fitness functions for the hexagonal layout (ADR-01, ADR-02, ADR-03).
 *
 * <p>Layering that is only written down in an ADR erodes one pragmatic import at
 * a time, and nobody notices until the domain needs a container to instantiate.
 * These rules make each of those imports a build failure with the reason
 * attached, which is the difference between a convention and a constraint.</p>
 *
 * <p>Every business component uses the {@code {bc}.domain / .application /
 * .adapter} layout. Cross-cutting primitives live in {@code kernel}.</p>
 */
class HexagonalArchitectureTest {

    static final String BASE_PACKAGE = "de.ingoschindler";

    /** The one package any BC may depend on. */
    static final String SHARED_PACKAGE = "kernel";

    static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS).importPackages(BASE_PACKAGE);

    /**
     * The core stays framework-free: neither domain nor application may know about
     * transport (REST), persistence (JPA/Hibernate) or API documentation.
     *
     * <p>This is the rule that keeps plain-JUnit tests possible. A domain type that
     * extends a Panache entity cannot be constructed without the runtime, so the
     * tests covering its rules get written as slow integration tests — or not at
     * all.</p>
     */
    @Test
    void coreIsFrameworkFree() {
        ArchRule rule = noClasses().that().resideInAnyPackage("..domain..", "..application..").should()
                .dependOnClassesThat()
                .resideInAnyPackage("jakarta.ws.rs..", "org.jboss.resteasy..", "jakarta.persistence..",
                        "io.quarkus.hibernate..", "io.quarkus.panache..", "org.hibernate..",
                        "org.eclipse.microprofile.openapi..")
                .because("domain and application form the core, which stays free of transport, persistence and"
                        + " documentation frameworks (ADR-01)");
        rule.check(CLASSES);
    }

    /**
     * Dependencies point inwards only. An adapter may map domain types; the core
     * may not reach back out to an adapter.
     */
    @Test
    void hexagonalArchitectureLayeringIsCorrect() {
        ArchRule rule = layeredArchitecture().consideringAllDependencies().layer("Adapter").definedBy("..adapter..")
                .layer("Application").definedBy("..application..").layer("Domain").definedBy("..domain..")

                .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()

                .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")

                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter")

                .because("the core cannot know its adapters; adapters may use domain types for mapping (ADR-01)");
        rule.check(CLASSES);
    }

    @Test
    void adaptersDoNotDependOnAnotherBcsDomain() {
        ArchRule rule = classes().that().resideInAPackage(BASE_PACKAGE + "..").and().resideInAPackage("..adapter..")
                .should(notDependOnAnotherBcDomain())
                .because("an adapter may only map its own BC's domain; cross-BC calls go through"
                        + " application.port.in (ADR-01, ADR-03)");
        rule.check(CLASSES);
    }

    /**
     * Inbound adapters call ports, never the implementing class. Depending on the
     * concrete {@code *Service} compiles and works, which is exactly why it needs a
     * rule: it quietly turns the published API of a BC into whichever methods that
     * class happens to expose.
     */
    @Test
    void inboundAdaptersOnlyAccessPortsNotUseCases() {
        ArchRule rule = noClasses().that().resideInAPackage("..adapter.in..").should().dependOnClassesThat()
                .resideInAPackage("..application.usecase..")
                .because("inbound adapters interact with the application layer exclusively through ports (ADR-01)");
        rule.check(CLASSES);
    }

    @Test
    void inboundAdaptersDoNotDependOnPersistenceAdapter() {
        ArchRule rule = noClasses().that().resideInAPackage("..adapter.in..").should().dependOnClassesThat()
                .resideInAPackage("..adapter.out.persistence..")
                .because("inbound adapters (REST, messaging, scheduled) must not reach into persistence (ADR-01)");
        rule.check(CLASSES);
    }

    @Test
    void jpaEntitiesLiveOnlyInPersistenceAdapter() {
        ArchRule rule = classes().that().areAnnotatedWith("jakarta.persistence.Entity").and()
                .resideInAPackage(BASE_PACKAGE + "..").should().resideInAPackage("..adapter.out.persistence..")
                .because("JPA entities are a persistence-adapter concern (ADR-01)");
        rule.check(CLASSES);
    }

    @Test
    void panacheEntityBaseLivesOnlyInPersistenceAdapter() {
        ArchRule rule = noClasses().that().resideOutsideOfPackage("..adapter.out.persistence..").and()
                .resideInAPackage(BASE_PACKAGE + "..").should()
                .beAssignableTo("io.quarkus.hibernate.orm.panache.PanacheEntityBase").orShould()
                .beAssignableTo("io.quarkus.hibernate.orm.panache.PanacheEntity")
                .because("Active Record stays an internal detail of the persistence adapter (ADR-01)");
        rule.check(CLASSES);
    }

    /**
     * Entity {@code equals}/{@code hashCode} policy.
     *
     * <p>The "give every entity a business-key {@code equals}/{@code hashCode}"
     * rule only bites for entities held in a {@link Set}, or compared before an id
     * is assigned. Writing those implementations for entities that are never in a
     * {@code Set} is a lot of code guarding a condition that does not exist — and
     * every one of them is a chance to get it subtly wrong.</p>
     *
     * <p>So instead of the implementations, this rule keeps the assumption honest:
     * map a {@code Set<SomeJpaEntity>} and the build fails, telling you to add
     * business-key equality first — which is the moment it starts to matter. A
     * composite-key {@code @IdClass} is the one place the JPA spec requires the
     * pair regardless, and that is a nested type rather than an entity.</p>
     */
    @Test
    void jpaEntitiesDoNotDeclareSetFields() {
        ArchRule rule = fields().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("JpaEntity")
                .should(new ArchCondition<JavaField>("not have a raw type assignable to java.util.Set") {
                    @Override
                    public void check(JavaField field, ConditionEvents events) {
                        // isAssignableTo, not an equality check against java.util.Set:
                        // an exact match sails past every Set subtype, and the rule then
                        // looks green while checking nothing.
                        if (field.getRawType().isAssignableTo(Set.class)) {
                            events.add(SimpleConditionEvent.violated(field,
                                    "Field " + field.getFullName() + " has raw type " + field.getRawType().getName()
                                            + ", which is assignable to java.util.Set — a JPA entity held in a Set"
                                            + " needs business-key equals/hashCode; add it to the entity before"
                                            + " introducing a Set field"));
                        } else {
                            events.add(SimpleConditionEvent.satisfied(field,
                                    "Field " + field.getFullName() + " does not have a Set raw type"));
                        }
                    }
                });
        rule.check(CLASSES);
    }

    @Test
    void panacheQueryStaysInPersistenceAndSanctionedBridges() {
        ArchRule rule = noClasses().that().resideInAPackage(BASE_PACKAGE + "..").and()
                .resideOutsideOfPackages("..adapter.out.persistence..", "..kernel.pagination..").should()
                .dependOnClassesThat().haveFullyQualifiedName("io.quarkus.hibernate.orm.panache.PanacheQuery")
                .because("PanacheQuery must not leak past the persistence adapter; the pagination bridge is the"
                        + " only exception (ADR-01, ADR-02)");
        rule.check(CLASSES);
    }

    @Test
    void applicationLayerTakesUploadedFileNotJaxRsFileUpload() {
        ArchRule rule = noClasses().that().resideInAPackage("..application..").should().dependOnClassesThat()
                .haveFullyQualifiedName("org.jboss.resteasy.reactive.multipart.FileUpload")
                .because("the application layer takes UploadedFile so it stays drivable without HTTP (ADR-01)");
        rule.check(CLASSES);
    }

    @Test
    void noLegacyBceLayersRemain() {
        ArchRule rule = noClasses().that().resideInAPackage(BASE_PACKAGE + "..").should()
                .resideInAnyPackage("..boundary..", "..control..", "..entity..")
                .because("packages are .domain / .application / .adapter, not boundary/control/entity (ADR-01)");
        rule.check(CLASSES);
    }

    /**
     * A BC's domain stays BC-private. Sharing goes through
     * {@code application.port.in} or through {@code kernel}.
     */
    @Test
    void bcDomainsDoNotDependOnOtherBcDomains() {
        ArchRule rule = classes().that().resideInAPackage(BASE_PACKAGE + "..").and().resideInAPackage("..domain..")
                .should(notDependOnAnotherBcDomain())
                .because("BC domains stay private; sharing goes through application.port.in or kernel (ADR-01)");
        rule.check(CLASSES);
    }

    static ArchCondition<JavaClass> notDependOnAnotherBcDomain() {
        return new ArchCondition<>("not depend on another BC's domain (except sanctioned shared types)") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String sourceBc = bcOf(source.getPackageName());
                source.getDirectDependenciesFromSelf().forEach(dep -> {
                    String target = dep.getTargetClass().getFullName();
                    if (!target.startsWith(BASE_PACKAGE + ".") || !target.contains(".domain.")) {
                        return;
                    }

                    String targetBc = bcOf(dep.getTargetClass().getPackageName());
                    if (sourceBc == null || targetBc == null || sourceBc.equals(targetBc)) {
                        return;
                    }
                    if (SHARED_PACKAGE.equals(targetBc)) {
                        return;
                    }

                    events.add(SimpleConditionEvent.violated(source, source.getFullName() + " (in " + sourceBc
                            + ") depends on " + target + " (in " + targetBc + ")"));
                });
            }
        };
    }

    /**
     * One BC's adapter must not reach into another's — neither its persistence
     * (that is how two BCs end up sharing a table and neither can be deployed on
     * its own again) nor its inbound side (that is how a messaging listener ends
     * up observing another BC's domain event directly instead of its published
     * port).
     */
    @Test
    void adaptersDoNotDependOnAnotherBcsAdapter() {
        ArchRule rule = classes().that().resideInAPackage(BASE_PACKAGE + "..").and().resideInAPackage("..adapter..")
                .should(notDependOnAnotherBcsAdapter())
                .because("cross-BC access goes through application.port.in, never another BC's concrete adapter"
                        + " — in or out (ADR-01, ADR-03)");
        rule.check(CLASSES);
    }

    static ArchCondition<JavaClass> notDependOnAnotherBcsAdapter() {
        return new ArchCondition<>("not depend on another BC's adapter classes") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String sourceBc = bcOf(source.getPackageName());
                source.getDirectDependenciesFromSelf().forEach(dep -> {
                    JavaClass targetClass = dep.getTargetClass();
                    String target = targetClass.getFullName();
                    if (!target.startsWith(BASE_PACKAGE + ".") || !targetClass.getPackageName().contains(".adapter.")) {
                        return;
                    }

                    String targetBc = bcOf(targetClass.getPackageName());
                    if (sourceBc == null || targetBc == null || sourceBc.equals(targetBc)) {
                        return;
                    }
                    if (SHARED_PACKAGE.equals(targetBc)) {
                        return;
                    }

                    events.add(SimpleConditionEvent.violated(source, source.getFullName() + " (in " + sourceBc
                            + ") depends on " + target + " (in " + targetBc + ")"));
                });
            }
        };
    }

    static String bcOf(String packageName) {
        if (packageName == null || !packageName.startsWith(BASE_PACKAGE + ".")) {
            return null;
        }
        String tail = packageName.substring(BASE_PACKAGE.length() + 1);
        int dot = tail.indexOf('.');
        return dot < 0 ? tail : tail.substring(0, dot);
    }

    /**
     * Code outside a {@code kernel} subpackage depends on that subpackage's port
     * interface, not on its concrete {@code @ApplicationScoped} bean. Interplay
     * within one subpackage (an adapter and the client it wraps) stays allowed.
     *
     * <p>A bypassed port is invisible in review: the injection compiles, the tests
     * pass, and the abstraction is simply gone.</p>
     */
    @Test
    void kernelCapabilitiesAreOnlyAccessedThroughPorts() {
        ArchRule rule = classes().that().resideInAPackage(BASE_PACKAGE + "..")
                .should(notDependOnAConcreteKernelBeanFromOutsideItsSubpackage())
                .because("outside a kernel subpackage, depend on its port interface rather than injecting the"
                        + " concrete bean (ADR-02)");
        rule.check(CLASSES);
    }

    static ArchCondition<JavaClass> notDependOnAConcreteKernelBeanFromOutsideItsSubpackage() {
        return new ArchCondition<>("not depend on a concrete kernel CDI bean from outside its own kernel subpackage") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String sourceSubpackage = kernelSubpackageOf(source.getPackageName());
                source.getDirectDependenciesFromSelf().forEach(dep -> {
                    JavaClass target = dep.getTargetClass();
                    String targetSubpackage = kernelSubpackageOf(target.getPackageName());
                    if (targetSubpackage == null || targetSubpackage.equals(sourceSubpackage)) {
                        return;
                    }
                    if (target.isInterface()
                            || !target.isAnnotatedWith("jakarta.enterprise.context.ApplicationScoped")) {
                        return;
                    }

                    events.add(SimpleConditionEvent.violated(source,
                            source.getFullName() + " depends on " + target.getFullName()
                                    + ", a concrete kernel CDI bean outside its own kernel subpackage — depend on"
                                    + " its port interface instead"));
                });
            }
        };
    }

    static String kernelSubpackageOf(String packageName) {
        String prefix = BASE_PACKAGE + "." + SHARED_PACKAGE + ".";
        if (packageName == null || !packageName.startsWith(prefix)) {
            return null;
        }
        String tail = packageName.substring(prefix.length());
        int dot = tail.indexOf('.');
        return dot < 0 ? tail : tail.substring(0, dot);
    }
}
