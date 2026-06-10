package com.thaiuniontour.common.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 애그리거트 루트 베이스 (선택적 DDD). Reservation·Payment 등 핵심 애그리거트가 상속한다.
 * 도메인 이벤트를 모아두면 애플리케이션 서비스가 트랜잭션 커밋 시 Outbox로 옮긴다.
 * (Transactional Outbox — reference-patterns.md). 경량 컨텍스트는 상속하지 않아도 된다.
 */
public abstract class AggregateRoot {

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearEvents() {
        domainEvents.clear();
    }
}
