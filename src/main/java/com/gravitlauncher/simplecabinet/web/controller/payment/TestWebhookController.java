package com.gravitlauncher.simplecabinet.web.controller.payment;

import com.gravitlauncher.simplecabinet.web.model.shop.Payment;
import com.gravitlauncher.simplecabinet.web.model.user.User;
import com.gravitlauncher.simplecabinet.web.service.payment.TestPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/test")
public class TestWebhookController {
    @Autowired
    private TestPaymentService service;

    @PostMapping("/payment")
    public ResponseEntity<Payment> payment(@RequestBody TestPaymentService.WebhookResponse webhookResponse, HttpServletRequest request) {
        // В реальном мире тут может быть проверка IP, подписи и т.д.
        service.complete(webhookResponse);
        return ResponseEntity.ok().build();
    }
}
