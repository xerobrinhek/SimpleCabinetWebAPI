package com.gravitlauncher.simplecabinet.web;

import com.gravitlauncher.simplecabinet.web.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class WebApplication {

    public static final String VERSION = "0.1.0";

    public static void main(String[] args) throws Exception {
        WebApplicationOnStartup.prepare();
        SpringApplication.run(WebApplication.class, args);
    }

    @Component
    public class TokenCleanupTask {
        @Autowired
        private PasswordResetService passwordResetService;

        @Scheduled(fixedRate = 30 * 60 * 1000) // каждые 30 минут
        public void cleanup() {
            passwordResetService.cleanupExpiredTokens();
        }
    }
}
