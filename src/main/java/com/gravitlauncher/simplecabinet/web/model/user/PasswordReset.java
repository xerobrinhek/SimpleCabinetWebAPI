// src/main/java/com/gravitlauncher/simplecabinet/web/model/user/PasswordReset.java
package com.gravitlauncher.simplecabinet.web.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity(name = "PasswordReset")
@Table(name = "password_resets")
public class PasswordReset {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "password_resets_generator")
    @SequenceGenerator(name = "password_resets_generator", sequenceName = "password_resets_seq", allocationSize = 1)
    private long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Setter
    @Column(unique = true)
    private UUID uuid;

    // НОВОЕ ПОЛЕ: время создания
    @Setter
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
