package com.dday.wedding.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 번역이 필요 없는 값만. 문구는 label 테이블. */
@Getter @Setter
@Component
@ConfigurationProperties("wedding")
public class WeddingProps {
    private String baseUrl = "";          // Origin 검사·og:url. 빈 값이면 Origin 검사 생략(로컬)
    private String dateTime;              // ISO-8601, 예 2027-05-15T12:00:00+09:00
    private double lat;
    private double lng;
    private String kakaoJsKey = "";
    private String turnstileSiteKey = "";
    private String turnstileSecret = "";
    private String masterPwHash = "";     // 방명록 마스터 비번 BCrypt 해시
    private Map<String, String> address = Map.of();          // lang → 상세주소
    private Map<String, Account> account = Map.of();         // groom|bride → 계좌

    @Getter @Setter
    public static class Account { private String holder; private String bank; private String number; }
}
