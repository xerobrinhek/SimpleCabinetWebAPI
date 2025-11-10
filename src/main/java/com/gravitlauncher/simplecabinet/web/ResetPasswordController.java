// src/main/java/com/gravitlauncher/simplecabinet/web/controller/ResetPasswordController.java
package com.gravitlauncher.simplecabinet.web.controller;

import com.gravitlauncher.simplecabinet.web.exception.EntityNotFoundException;
import com.gravitlauncher.simplecabinet.web.model.user.PasswordReset;
import com.gravitlauncher.simplecabinet.web.model.user.User;
import com.gravitlauncher.simplecabinet.web.repository.user.PasswordResetRepository;
import com.gravitlauncher.simplecabinet.web.service.mail.MailService;
import com.gravitlauncher.simplecabinet.web.service.user.PasswordCheckService;
import com.gravitlauncher.simplecabinet.web.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class ResetPasswordController {

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

    // 1. Запрос на сброс: username + email
    @PostMapping("/resetpass")
    public ResponseEntity<Void> requestReset(@RequestBody ResetRequest request) {
        var userOpt = userService.findByUsername(request.username);
        if (userOpt.isEmpty()) {
            // НЕ раскрываем, что username не существует
            return ResponseEntity.ok().build();
        }

        var user = userOpt.get();
        if (!user.getEmail().equalsIgnoreCase(request.email)) {
            // НЕ раскрываем, что email не совпадает
            return ResponseEntity.ok().build();
        }

        // Удаляем старые токены
        passwordResetRepository.deleteByUserId(user.getId());

        // Генерируем токен
        UUID token = UUID.randomUUID();
        PasswordReset reset = new PasswordReset();
        reset.setUser(user);
        reset.setUuid(token);
        passwordResetRepository.save(reset);

        // Отправляем письмо
        String resetUrl = String.format(resetPasswordUrlTemplate, token);
        mailService.sendTemplateEmail(
            user.getEmail(),
            "email-passwordreset.html",
            "%username%", user.getUsername(),
            "%url%", resetUrl
        );

        return ResponseEntity.ok().build();
    }

    // 2. Установка нового пароля
    @PostMapping("/resetpass/{token}")
    public ResponseEntity<Void> resetPassword(@PathVariable String token, @RequestBody ResetPasswordRequest request) {
        UUID uuid;
        try {
            uuid = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new EntityNotFoundException("Invalid token");
        }

        var resetOpt = passwordResetRepository.findByUuid(uuid);
        if (resetOpt.isEmpty()) {
            throw new EntityNotFoundException("Invalid or expired token");
        }

        var reset = resetOpt.get();
        var user = reset.getUser();

        // Устанавливаем новый пароль
        passwordCheckService.setPassword(user, request.newPassword);
        userService.save(user);

        // Удаляем токен
        passwordResetRepository.delete(reset);

        return ResponseEntity.ok().build();
    }

    public record ResetRequest(String username, String email) {}
    public record ResetPasswordRequest(String newPassword) {}
}
