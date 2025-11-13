// src/main/java/com/gravitlauncher/simplecabinet/web/controller/ResetPasswordController.java
package com.gravitlauncher.simplecabinet.web.controller;

import com.gravitlauncher.simplecabinet.web.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ResetPasswordController {

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/resetpass")
    public ResetResponse requestResetTest(@RequestBody ResetRequest request) {
        passwordResetService.cleanupExpiredTokens();
        var result = passwordResetService.requestPasswordReset(request.username(), request.email());
        return new ResetResponse(result.done(), result.correct());
    }
    @PostMapping("/resetpass/{token}")
    public ResetConfirmResponse resetPasswordTest(@PathVariable String token, @RequestBody ResetPasswordRequest request) {
        var result = passwordResetService.completePasswordReset(token, request.newPassword());
        passwordResetService.cleanupExpiredTokens();
        return new ResetConfirmResponse(result.done(), result.correct());
    }

    public record ResetConfirmResponse(boolean done, boolean correct) {}
    public record ResetResponse(boolean done, boolean correct) {}
    public record ResetRequest(String username, String email) {}
    public record ResetPasswordRequest(String newPassword) {}
}
