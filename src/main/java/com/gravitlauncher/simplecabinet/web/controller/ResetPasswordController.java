// src/main/java/com/gravitlauncher/simplecabinet/web/controller/ResetPasswordController.java
package com.gravitlauncher.simplecabinet.web.controller;

import com.gravitlauncher.simplecabinet.web.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ResetPasswordController {

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/resetpass")
    public ResponseEntity<Void> requestReset(@RequestBody ResetRequest request) {
        passwordResetService.requestPasswordReset(request.username(), request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resetpass/{token}")
    public ResponseEntity<Void> resetPassword(@PathVariable String token, @RequestBody ResetPasswordRequest request) {
        passwordResetService.completePasswordReset(token, request.newPassword());
        return ResponseEntity.ok().build();
    }

    public record ResetRequest(String username, String email) {}
    public record ResetPasswordRequest(String newPassword) {}
}
