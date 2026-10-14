package com.dday.family.auth;

import com.dday.family.common.Times;
import com.dday.family.security.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** audit_log 한 줄 (설계 §9). 승인·역할 변경·삭제·원본 다운로드·로그인 실패. */
@Component
@RequiredArgsConstructor
public class Audit {
    private final JdbcTemplate jdbc;

    public void log(Long familyId, Long memberId, String action, String target, HttpServletRequest req) {
        jdbc.update("INSERT INTO audit_log(family_id, member_id, action, target, ip, created_at) VALUES (?,?,?,?,?,?)",
                familyId, memberId, action, target, req == null ? null : ClientIp.of(req), Times.now());
    }

    public void log(Me me, String action, String target, HttpServletRequest req) {
        log(me.familyId(), me.memberId(), action, target, req);
    }
}
