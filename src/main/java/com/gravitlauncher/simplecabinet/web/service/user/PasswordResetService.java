// src/main/java/com/gravitlauncher/simplecabinet/web/service/PasswordResetService.java
package com.gravitlauncher.simplecabinet.web.service;

import com.gravitlauncher.simplecabinet.web.exception.EntityNotFoundException;
import com.gravitlauncher.simplecabinet.web.exception.InvalidParametersException;
import com.gravitlauncher.simplecabinet.web.model.user.PasswordReset;
import com.gravitlauncher.simplecabinet.web.model.user.User;
import com.gravitlauncher.simplecabinet.web.repository.user.PasswordResetRepository;
import com.gravitlauncher.simplecabinet.web.service.mail.MailService;
import com.gravitlauncher.simplecabinet.web.service.user.PasswordCheckService;
import com.gravitlauncher.simplecabinet.web.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Value("${reset.password.url:https://optically-serene-primate.cloudpub.ru/resetpass/%s}")
    private String resetPasswordUrlTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private PasswordCheckService passwordCheckService;

    private static final long TOKEN_EXPIRY_HOURS = 1;

    @Transactional
    public void requestPasswordReset(String username, String email) {
        var userOpt = userService.findByUsername(username);
        if (userOpt.isEmpty() || !userOpt.get().getEmail().equalsIgnoreCase(email)) {
            // Всегда возвращаем "успех", чтобы не раскрывать валидность данных
            return;
        }

        // Удаляем старые токены (в т.ч. просроченные)
        passwordResetRepository.deleteByUserId(userOpt.get().getId());

        // Создаём новый токен
        PasswordReset reset = new PasswordReset();
        reset.setUser(userOpt.get());
        reset.setUuid(UUID.randomUUID());
        reset.setCreatedAt(LocalDateTime.now());
        passwordResetRepository.save(reset);

        // Отправляем письмо
        String resetUrl = String.format(resetPasswordUrlTemplate, reset.getUuid());
        mailService.sendTemplateEmail(
            userOpt.get().getEmail(),
            "email-passwordreset.html",
            "%username%", userOpt.get().getUsername(),
            "%url%", resetUrl
        );
    }

    @Transactional
    public void completePasswordReset(String tokenStr, String newPassword) {
        UUID token;
        try {
            token = UUID.fromString(tokenStr);
        } catch (Exception e) {
            throw new InvalidParametersException("Invalid token format", 400);
        }

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(TOKEN_EXPIRY_HOURS);
        var resetOpt = passwordResetRepository.findByUuidAndNotExpired(token, oneHourAgo);

        if (resetOpt.isEmpty()) {
            throw new InvalidParametersException("Invalid or expired token", 401);
        }

        var reset = resetOpt.get();
        var user = reset.getUser();

        // Меняем пароль
        passwordCheckService.setPassword(user, newPassword);
        userService.save(user);

        // Удаляем токен (одноразовый)
        passwordResetRepository.delete(reset);
    }

    // Опционально: метод для фоновой очистки (можно вызывать по расписанию)
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(TOKEN_EXPIRY_HOURS);
        passwordResetRepository.deleteExpired(oneHourAgo);
    }
  }
