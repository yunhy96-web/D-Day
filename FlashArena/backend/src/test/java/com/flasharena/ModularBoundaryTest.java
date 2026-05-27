package com.flasharena;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 모듈러 모놀리스 경계를 빌드 타임에 강제하는 ArchUnit 규칙.
 * <p>경계는 "규약"이 아니라 "테스트로 강제되는 불변식"이다 — 위반하면 빌드가 실패한다.
 * 도메인(order/payment/auth)은 서로의 내부(infrastructure/domain)를 직접 건드릴 수 없고,
 * 교차 통신은 공개 application 포트 또는 이벤트(Redis Stream)로만 한다.
 */
@AnalyzeClasses(packages = "com.flasharena", importOptions = ImportOption.DoNotIncludeTests.class)
class ModularBoundaryTest {

    /** payment 는 order 를 절대 모른다 (단방향 의존 — 이벤트로만 받는다). */
    @ArchTest
    static final ArchRule payment_must_not_depend_on_order =
            noClasses()
                    .that().resideInAPackage("..payment..")
                    .should().dependOnClassesThat().resideInAPackage("..order..");

    /** order 는 payment 의 내부(infrastructure/domain)에 접근하지 않는다 — 공개 application 포트만 허용. */
    @ArchTest
    static final ArchRule order_must_only_touch_payment_via_public_api =
            noClasses()
                    .that().resideInAPackage("..order..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..payment.infrastructure..", "..payment.domain..");

    /** auth 는 다른 도메인을 모르는 독립 모듈이다. */
    @ArchTest
    static final ArchRule auth_must_not_depend_on_other_domains =
            noClasses()
                    .that().resideInAPackage("..auth..")
                    .should().dependOnClassesThat().resideInAnyPackage("..order..", "..payment..");

    /** 도메인 계층은 인프라/표현 계층에 의존하지 않는다 (의존성 역전 — domain 이 가장 안쪽). */
    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure_or_presentation =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..infrastructure..", "..presentation..");

    /** 도메인 슬라이스 간 순환 의존이 없어야 한다. */
    @ArchTest
    static final ArchRule domains_must_be_free_of_cycles =
            slices()
                    .matching("com.flasharena.(order|payment|auth)..")
                    .should().beFreeOfCycles();
}
