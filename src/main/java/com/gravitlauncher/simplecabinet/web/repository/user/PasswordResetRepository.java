// src/main/java/com/gravitlauncher/simplecabinet/web/repository/user/PasswordResetRepository.java
package com.gravitlauncher.simplecabinet.web.repository.user;

import com.gravitlauncher.simplecabinet.web.model.user.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    // Найти токен, если он не старше 1 часа
    @Query("SELECT p FROM PasswordReset p WHERE p.uuid = :uuid AND p.createdAt > :oneHourAgo")
    Optional<PasswordReset> findByUuidAndNotExpired(UUID uuid, LocalDateTime oneHourAgo);

    // Удалить все токены старше 1 часа (для очистки)
    @Modifying
    @Query("DELETE FROM PasswordReset p WHERE p.createdAt < :oneHourAgo")
    void deleteExpired(LocalDateTime oneHourAgo);
}
