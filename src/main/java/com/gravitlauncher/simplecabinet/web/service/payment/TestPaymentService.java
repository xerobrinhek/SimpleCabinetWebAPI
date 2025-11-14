package com.gravitlauncher.simplecabinet.web.service.payment;

import com.gravitlauncher.simplecabinet.web.configuration.properties.payment.TestPaymentConfig;
import com.gravitlauncher.simplecabinet.web.exception.PaymentException;
import com.gravitlauncher.simplecabinet.web.model.shop.Payment;
import com.gravitlauncher.simplecabinet.web.model.user.User;
import com.gravitlauncher.simplecabinet.web.service.shop.PaymentService;
import com.gravitlauncher.simplecabinet.web.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;

@Service
public class TestPaymentService implements BasicPaymentService {
    private final transient HttpClient client = HttpClient.newBuilder().build();
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private UserService userService;
    @Autowired
    private TestPaymentConfig config;

    @Override
    public PaymentService.PaymentCreationInfo createBalancePayment(User user, double sum, String ip) {
        if (!config.isEnable()) {
            throw new PaymentException("This payment method is disabled", 6);
        }
        var payment = paymentService.createBasic(user, sum);
        payment.setSystem("Test");
        paymentService.save(payment);

        // Возвращаем URL, куда пользователь будет направлен для "оплаты"
        String redirectUrl = String.format("%s?id=%s&sum=%s", config.getUrl(), payment.getId(), payment.getSum());
        return new PaymentService.PaymentCreationInfo(new PaymentService.PaymentRedirectInfo(redirectUrl), payment);
    }

    @Override
    public boolean isEnabled() {
        return config.isEnable();
    }

    public void complete(WebhookResponse webhookResponse) {
        // Логируем пришедший ID для отладки
        System.out.println("Получен вебхук с ID: " + webhookResponse.id());

        // Ищем платеж
        var paymentOpt = paymentService.findUserPaymentById(Long.parseLong(webhookResponse.id()));

        if (paymentOpt.isEmpty()) {
            // Лучше выбросить специфическое исключение
            throw new PaymentException("Payment not found for systemPaymentId: " + webhookResponse.id(), 1001);
        }

        var payment = paymentOpt.get();

        var oldStatus = payment.getStatus();
        if (webhookResponse.status().equalsIgnoreCase("SUCCESS")) {
            completePayment(payment, Payment.PaymentStatus.SUCCESS);
            if (oldStatus != Payment.PaymentStatus.SUCCESS) {
                paymentService.deliveryPayment(payment);
            }
        } else {
            completePayment(payment, Payment.PaymentStatus.CANCELED);
        }
    }

    private void completePayment(Payment payment, Payment.PaymentStatus status) {
        payment.setStatus(status);
        paymentService.save(payment);
    }

    public record WebhookResponse(String id, String status) {

    }
}
