package com.checkstockbackend.service;

import com.checkstockbackend.dto.ProductDto;
import com.checkstockbackend.entity.MonitoredSite;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TelegramService {

    @Value("${checkstock.telegram.bot-token:}")
    private String botToken;

    @Value("${checkstock.telegram.chat-id:}")
    private String chatId;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendNewMatchAlert(MonitoredSite site, List<ProductDto> added, int totalMatched) {
        if (botToken.isEmpty() || chatId.isEmpty()) {
            log.warn("텔레그램 설정이 없어 알림을 보내지 않습니다.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🆕 <b>").append(site.getName()).append("</b>\n\n");
        sb.append("새 매칭 상품 <b>").append(added.size()).append("개</b> 발견! (총 ").append(totalMatched).append("개)\n\n");

        for (ProductDto p : added) {
            sb.append("• <a href=\"").append(p.getUrl()).append("\">").append(escapeHtml(p.getName())).append("</a>\n");
        }

        sendMessage(sb.toString());
    }

    public void sendErrorAlert(String siteName, String errorMessage) {
        String msg = "🚨 <b>크롤링 에러</b>\n\n📦 " + siteName + "\n❗ " + errorMessage;
        sendMessage(msg);
    }

    public void sendMessage(String text) {
        if (botToken.isEmpty() || chatId.isEmpty()) return;

        try {
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "HTML");
            body.put("disable_web_page_preview", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);
            log.info("텔레그램 알림 전송 완료");
        } catch (Exception e) {
            log.error("텔레그램 알림 전송 실패: {}", e.getMessage());
        }
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
