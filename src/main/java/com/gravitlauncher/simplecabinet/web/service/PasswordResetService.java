package com.gravitlauncher.simplecabinet.web.service;

import com.gravitlauncher.simplecabinet.web.configuration.properties.PasswordResetConfig;
import com.gravitlauncher.simplecabinet.web.configuration.properties.RegistrationConfig;
import com.gravitlauncher.simplecabinet.web.model.user.PasswordReset;
import com.gravitlauncher.simplecabinet.web.repository.user.PasswordResetRepository;
import com.gravitlauncher.simplecabinet.web.service.mail.MailService;
import com.gravitlauncher.simplecabinet.web.service.user.PasswordCheckService;
import com.gravitlauncher.simplecabinet.web.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@ConditionalOnProperty(
        value = "resetpass.enabled")
public class PasswordResetService {

    @Autowired
    private RegistrationConfig configPass;

    @Autowired
    private PasswordResetConfig config;

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
    public ResetRequestResult requestPasswordReset(String username, String email) {
        var userOpt = userService.findByUsername(username);
        if (userOpt.isEmpty() || !userOpt.get().getEmail().equalsIgnoreCase(email)) {
            return new ResetRequestResult(false, "Указан неправильный email или ник!");
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
        String resetUrl = String.format(config.getUrl(), reset.getUuid());
        mailService.sendTemplateEmail(
            userOpt.get().getEmail(),
            "email-passwordreset.html",
            "%username%", URLEncoder.encode(userOpt.get().getUsername(), StandardCharsets.UTF_8),
            "%url%", resetUrl
        );
        return new ResetRequestResult(true, "Ссылка со сбросом пароля отправлена на ваш email.");
    }

    @Transactional
    public ResetConfirmResult completePasswordReset(String tokenStr, String newPassword) {
        UUID token;
        try {
            token = UUID.fromString(tokenStr);
        } catch (Exception e) {
            return new ResetConfirmResult(false,"Неправильный формат токена");
        }

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(TOKEN_EXPIRY_HOURS);
        var resetOpt = passwordResetRepository.findByUuidAndNotExpired(token, oneHourAgo);

        if (resetOpt.isEmpty()) {
            return new ResetConfirmResult(false,"Неправильный или просроченный токен");
        }

        var reset = resetOpt.get();
        var user = reset.getUser();

        // Меняем пароль
        if (newPassword.isEmpty()) {
            return new ResetConfirmResult(false,"Пустой пароль");
        }
        if (newPassword.length() < configPass.getMinPasswordLength() || newPassword.length() > configPass.getMaxPasswordLength()) {
            return new ResetConfirmResult(false,String.format("Длина пароля должна быть от %d до %d символов",
                    configPass.getMinPasswordLength(), configPass.getMaxPasswordLength()));
        } else {
            passwordCheckService.setPassword(user, newPassword);
            userService.save(user);
            passwordResetRepository.delete(reset);
            return new ResetConfirmResult(true, "Пароль успешно изменён! Теперь вы можете войти.");
        }
    }

    // Опционально: метод для фоновой очистки (можно вызывать по расписанию)
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(TOKEN_EXPIRY_HOURS);
        passwordResetRepository.deleteExpired(oneHourAgo);
    }
    public record ResetRequestResult(boolean done, String desc) {}
    public record ResetConfirmResult(boolean done, String desc) {}
  }
