package com.checkstock.service;

import com.checkstock.dto.SizeOptionDto;
import com.checkstock.entity.Product;
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
import java.util.stream.Collectors;

@Service
@Slf4j
public class TelegramService {

    @Value("${checkstock.telegram.bot-token:}")
    private String botToken;

    @Value("${checkstock.telegram.chat-id:}")
    private String chatId;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendChangeAlert(Product product,
                                List<SizeOptionDto> becameAvailable,
                                List<SizeOptionDto> becameSoldOut,
                                int oldAvailable, int newAvailable, int total) {
        if (botToken.isEmpty() || chatId.isEmpty()) {
            log.warn("텔레그램 설정이 없어 알림을 보내지 않습니다.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📦 <b>").append(product.getName()).append("</b>\n\n");

        if (!becameAvailable.isEmpty()) {
            sb.append("✅ <b>구매가능 사이즈</b>\n");
            String availSizes = becameAvailable.stream()
                    .map(s -> s.getGroup() != null ? s.getLabel() + "(" + s.getGroup() + ")" : s.getLabel())
                    .collect(Collectors.joining(", "));
            sb.append(availSizes).append("\n\n");
        }

        if (!becameSoldOut.isEmpty()) {
            sb.append("❌ <b>품절 사이즈</b>\n");
            String soldSizes = becameSoldOut.stream()
                    .map(s -> s.getGroup() != null ? s.getLabel() + "(" + s.getGroup() + ")" : s.getLabel())
                    .collect(Collectors.joining(", "));
            sb.append(soldSizes).append("\n\n");
        }

        sb.append("📊 ").append(oldAvailable).append(" → ").append(newAvailable)
                .append(" / ").append(total).append("개\n\n");
        sb.append("<a href=\"").append(product.getUrl()).append("\">상품 페이지 바로가기</a>");

        sendMessage(sb.toString());
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
}
