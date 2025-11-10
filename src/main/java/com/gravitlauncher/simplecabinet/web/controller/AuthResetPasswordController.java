package com.gravitlauncher.simplecabinet.web.controller;

import com.gravitlauncher.simplecabinet.web.exception.EntityNotFoundException;
import com.gravitlauncher.simplecabinet.web.exception.InvalidParametersException;
import com.gravitlauncher.simplecabinet.web.model.user.PasswordReset;
import com.gravitlauncher.simplecabinet.web.model.user.User;
import com.gravitlauncher.simplecabinet.web.service.mail.MailService;
import com.gravitlauncher.simplecabinet.web.service.user.PasswordCheckService;
import com.gravitlauncher.simplecabinet.web.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class AuthResetPasswordController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordCheckService passwordCheckService;

    @Autowired
    private MailService mailService;

    @Value("${reset.password.url:https://ваш-сайт/resetpass/%s}")
    private String resetPasswordUrlTemplate;

    // 1. Запрос на сброс: логин + email
    @PostMapping("/resetpass")
    public void requestReset(@RequestBody ResetRequest request) {
        var userOpt = userService.findByUsername(request.username);
        if (userOpt.isEmpty()) {
            // НЕ раскрываем, что username не существует
            return;
        }
        var user = userOpt.get();

        if (!user.getEmail().equalsIgnoreCase(request.email)) {
            // НЕ раскрываем, что email не совпадает
            return;
        }

        // Генерируем токен
        UUID token = UUID.randomUUID();

        // Сохраняем в БД
        PasswordReset reset = new PasswordReset();
        reset.setUser(user);
        reset.setUuid(token);
        userService.savePasswordReset(reset);

        // Отправляем письмо
        String resetUrl = String.format(resetPasswordUrlTemplate, token);
        mailService.sendTemplateEmail(
            user.getEmail(),
            "email-passwordreset.html",
            "%username%", user.getUsername(),
            "%url%", resetUrl
        );
    }

    // 2. Установка нового пароля по токену
    @PostMapping("/resetpass/{token}")
    public void resetPassword(@PathVariable String token, @RequestBody ResetPasswordRequest request) {
        UUID uuid;
        try {
            uuid = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new InvalidParametersException("Invalid token format", 400);
        }

        var resetOpt = userService.findPasswordResetByUuid(uuid);
        if (resetOpt.isEmpty()) {
            throw new InvalidParametersException("Invalid or expired token", 401);
        }

        var reset = resetOpt.get();
        var user = reset.getUser();

        // Устанавливаем новый пароль
        passwordCheckService.setPassword(user, request.newPassword);
        userService.save(user);

        // Удаляем токен после использования
        userService.deletePasswordReset(reset);

        // Логируем событие (опционально)
        // auditService.create(AUDIT_TYPE_PASSWORD_RESET, user);
    }

    // DTO
    public record ResetRequest(String username, String email) {}
    public record ResetPasswordRequest(String newPassword) {}
          }
