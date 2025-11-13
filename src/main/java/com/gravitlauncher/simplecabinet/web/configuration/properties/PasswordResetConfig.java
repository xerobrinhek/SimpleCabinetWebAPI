package com.gravitlauncher.simplecabinet.web.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "reset.password")
public class PasswordResetConfig {
    private String url;

    public PasswordResetConfig() {
    }

}
