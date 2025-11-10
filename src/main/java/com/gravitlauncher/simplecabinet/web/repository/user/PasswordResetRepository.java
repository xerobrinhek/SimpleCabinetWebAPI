// src/main/java/com/gravitlauncher/simplecabinet/web/repository/user/PasswordResetRepository.java
package com.gravitlauncher.simplecabinet.web.repository.user;

import com.gravitlauncher.simplecabinet.web.model.user.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    // Найти по UUID (основной метод для сброса)
    Optional<PasswordReset> findByUuid(UUID uuid);

    // Удалить все токены пользователя (например, после смены пароля)
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordReset pr WHERE pr.user.id = :userId")
    void deleteByUserId(Long userId);
}
