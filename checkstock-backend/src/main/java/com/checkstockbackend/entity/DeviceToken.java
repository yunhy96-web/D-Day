package com.checkstockbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_tokens")
@Getter
@Setter
@NoArgsConstructor
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 512)
    private String expoPushToken;

    private String platform; // ios, android

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    private LocalDateTime lastSeenAt;

    @PrePersist
    public void prePersist() {
        if (registeredAt == null) registeredAt = LocalDateTime.now();
        if (lastSeenAt == null) lastSeenAt = registeredAt;
    }
}
