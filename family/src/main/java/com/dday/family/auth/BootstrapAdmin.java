package com.dday.family.auth;

import com.dday.family.common.AppProps;
import com.dday.family.common.Times;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** 기동 시: 가족이 없으면 1행, 회원이 0명이면 .env 의 해시로 최초 관리자 (설계 §4). */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapAdmin {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final AppProps props;

    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        tx.executeWithoutResult(s -> {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM family", Long.class) == 0)
                jdbc.update("INSERT INTO family(id, name) VALUES (?, ?)", AuthService.DEFAULT_FAMILY, props.getFamilyName());
            if (jdbc.queryForObject("SELECT COUNT(*) FROM member", Long.class) > 0) return;
            if (props.getAdminPwHash().isBlank()) { log.warn("회원 0명인데 ADMIN_PW_HASH 가 비어 있어 관리자를 만들지 않음"); return; }
            Long id = jdbc.queryForObject("INSERT INTO member(login_id, password_hash, name, color) VALUES (?,?,?, 'blue') RETURNING id",
                    Long.class, props.getAdminLoginId(), props.getAdminPwHash(), "관리자");
            jdbc.update("INSERT INTO family_member(family_id, member_id, status) VALUES (?,?, 'ACTIVE')", AuthService.DEFAULT_FAMILY, id);
            jdbc.update("INSERT INTO member_role(family_id, member_id, role_code, granted_at) VALUES (?,?, 'ADMIN', ?)", AuthService.DEFAULT_FAMILY, id, Times.now());
            log.info("최초 관리자 생성: {}", props.getAdminLoginId());
        });
    }
}
