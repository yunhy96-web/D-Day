package com.dday.family.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("app")
public class AppProps {
    private String dataDir;
    private String baseUrl = "";
    private boolean cookieSecure;
    private String familyName;
    private String adminLoginId;
    private String adminPwHash = "";
    private Storage storage = new Storage();

    @Data
    public static class Storage {
        private String endpoint, region, bucket;
        private String accessKey = "", secretKey = "";
    }
}
