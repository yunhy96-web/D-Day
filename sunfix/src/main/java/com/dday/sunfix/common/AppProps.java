package com.dday.sunfix.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("app")
public class AppProps {
    private String dataDir;
    private String baseUrl = "";
    private boolean cookieSecure;
    private String adminLoginId;
    private String adminPwHash = "";
}
