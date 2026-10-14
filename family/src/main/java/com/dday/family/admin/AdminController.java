package com.dday.family.admin;

import com.dday.family.auth.Audit;
import com.dday.family.auth.AuthService;
import com.dday.family.auth.Me;
import com.dday.family.auth.Perm;
import com.dday.family.common.BadRequest;
import com.dday.family.common.NotFound;
import com.dday.family.common.Times;
import com.dday.family.common.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** 회원 승인·차단·역할·비밀번호 초기화 (설계 §4·§10). 바꾸는 즉시 저장 + audit_log. */
@RestController
@RequiredArgsConstructor
public class AdminController {
    private static final Set<String> STATUSES = Set.of("ACTIVE", "BLOCKED");
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final AuthService auth;
    private final Audit audit;

    public record StatusBody(String status) {}
    public record RoleBody(String role, Boolean granted) {}

    /** 승인(PENDING→ACTIVE, 역할이 없으면 MEMBER) · 차단 · 차단 해제 */
    @PutMapping("/admin/api/members/{id}/status")
    public ResponseEntity<Void> status(@PathVariable long id, @RequestBody StatusBody b, Me me, HttpServletRequest req) {
        String st = Valid.oneOf(b.status(), STATUSES, "status");
        String cur = current(me, id);
        if (cur.equals("PENDING")) me.require(Perm.MEMBER_APPROVE); else me.require(Perm.MEMBER_MANAGE);
        if (id == me.memberId()) throw new BadRequest("self");
        tx.executeWithoutResult(s -> {
            jdbc.update("UPDATE family_member SET status=? WHERE family_id=? AND member_id=?", st, me.familyId(), id);
            if (st.equals("ACTIVE") && jdbc.queryForObject("SELECT COUNT(*) FROM member_role WHERE family_id=? AND member_id=?", Integer.class, me.familyId(), id) == 0)
                jdbc.update("INSERT INTO member_role(family_id, member_id, role_code, granted_by, granted_at) VALUES (?,?, 'MEMBER', ?, ?)",
                        me.familyId(), id, me.memberId(), Times.now());
            if (st.equals("BLOCKED")) auth.killSessions(id);
        });
        audit.log(me, cur.equals("PENDING") ? "member.approve" : "member.status." + st, "member:" + id, req);
        return ResponseEntity.noContent().build();
    }

    /** 가입 거절: 대기 중인 신청만 완전히 지운다(아이디 재사용 가능) */
    @DeleteMapping("/admin/api/members/{id}")
    public ResponseEntity<Void> reject(@PathVariable long id, Me me, HttpServletRequest req) {
        me.require(Perm.MEMBER_APPROVE);
        if (!current(me, id).equals("PENDING")) throw new BadRequest("notPending");
        tx.executeWithoutResult(s -> {
            auth.killSessions(id);
            jdbc.update("DELETE FROM family_member WHERE family_id=? AND member_id=?", me.familyId(), id);
            jdbc.update("DELETE FROM member WHERE id=? AND NOT EXISTS(SELECT 1 FROM family_member WHERE member_id=?)", id, id);
        });
        audit.log(me, "member.reject", "member:" + id, req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/api/members/{id}/roles")
    public ResponseEntity<Void> role(@PathVariable long id, @RequestBody RoleBody b, Me me, HttpServletRequest req) {
        me.require(Perm.ROLE_MANAGE);
        current(me, id);
        if (b.granted() == null || jdbc.queryForObject("SELECT COUNT(*) FROM role WHERE code=?", Integer.class, b.role()) == 0) throw new BadRequest("role");
        if (id == me.memberId() && "ADMIN".equals(b.role()) && !b.granted()) throw new BadRequest("self");
        if (b.granted())
            jdbc.update("INSERT OR IGNORE INTO member_role(family_id, member_id, role_code, granted_by, granted_at) VALUES (?,?,?,?,?)",
                    me.familyId(), id, b.role(), me.memberId(), Times.now());
        else
            jdbc.update("DELETE FROM member_role WHERE family_id=? AND member_id=? AND role_code=?", me.familyId(), id, b.role());
        audit.log(me, b.granted() ? "role.grant" : "role.revoke", "member:" + id + ":" + b.role(), req);
        return ResponseEntity.noContent().build();
    }

    /** 임시 비밀번호는 응답에 한 번만. 다음 로그인 때 변경 강제 */
    @PostMapping("/admin/api/members/{id}/reset-password")
    public Map<String, String> reset(@PathVariable long id, Me me, HttpServletRequest req) {
        me.require(Perm.MEMBER_MANAGE);
        current(me, id);
        String temp = AuthService.tempPassword();
        jdbc.update("UPDATE member SET password_hash=?, must_change_pw='Y' WHERE id=?", AuthService.hash(temp), id);
        auth.killSessions(id);
        audit.log(me, "member.resetPassword", "member:" + id, req);
        return Map.of("tempPassword", temp);
    }

    private String current(Me me, long id) {
        List<String> r = jdbc.queryForList("SELECT status FROM family_member WHERE family_id=? AND member_id=?", String.class, me.familyId(), id);
        if (r.isEmpty()) throw new NotFound();
        return r.get(0);
    }
}
