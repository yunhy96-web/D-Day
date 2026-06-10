/**
 * Member Bounded Context (경량 + Role/게스트 VO). 회원·권한.
 *
 * <p>Role 기반: MEMBER/ADMIN (+ 확장 PARTNER). 비회원 예약은 회원 없이 Reserver 정보로 처리.
 * <p>4계층 컨벤션. 엔티티는 {@code @Table(schema = "member")} 명시 필수.
 */
package com.thaiuniontour.domain.member;
