package com.flasharena.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * auth.users 매핑 엔티티. 공용 관리자 계정 1개를 표현한다.
 * PK 는 DB 의 gen_random_uuid() 와 동일하게 UUID 로 생성한다.
 */
@Entity
@Table(name = "users", schema = "auth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    // BCrypt 해시. 평문 저장 금지.
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    // DB default now() 가 채우므로 insert 시 값을 보내지 않는다.
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    private User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
}
