package DdaySpringBoot.domain.article.application;

import DdaySpringBoot.domain.article.domain.Article;
import DdaySpringBoot.domain.article.domain.ArticleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    private final ArticleRepository articleRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    @Transactional
    public void translateArticle(Long articleNo) {
        try {
            Article article = articleRepository.findById(articleNo)
                    .orElseThrow(() -> new RuntimeException("Article not found: " + articleNo));

            article.setTranslationStatus("PROCESSING");
            articleRepository.save(article);

            String title = article.getTitle();
            String content = article.getContent();

            // 언어 감지 및 번역 요청
            String prompt = buildTranslationPrompt(title, content);
            String response = callOpenAI(prompt);

            // 응답 파싱
            TranslationResult result = parseTranslationResponse(response);

            // 결과 저장
            article.updateTranslation(
                    result.originalLang,
                    result.titleKo,
                    result.contentKo,
                    result.titleTh,
                    result.contentTh
            );
            articleRepository.save(article);

            log.info("Translation completed for article: {}", articleNo);

        } catch (Exception e) {
            log.error("Translation failed for article: {}", articleNo, e);
            articleRepository.findById(articleNo).ifPresent(article -> {
                article.setTranslationStatus("FAILED");
                articleRepository.save(article);
            });
        }
    }

    private String buildTranslationPrompt(String title, String content) {
        return """
            Detect the original language and translate the following text to both Korean and Thai.

            Original Title: %s
            Original Content: %s

            Rules:
            - If original is Korean: translate to Thai only, set title_ko and content_ko to null
            - If original is Thai: translate to Korean only, set title_th and content_th to null
            - If original is other language: translate to both Korean and Thai

            Respond ONLY with valid JSON (no markdown, no explanation):
            {
                "original_lang": "ko or th or other",
                "title_ko": "Korean title or null if original is Korean",
                "content_ko": "Korean content or null if original is Korean",
                "title_th": "Thai title or null if original is Thai",
                "content_th": "Thai content or null if original is Thai"
            }
            """.formatted(title, content);
    }

    private String callOpenAI(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a professional translator. Always respond in valid JSON format only."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
        );

        String response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return response;
    }

    private TranslationResult parseTranslationResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // JSON 부분만 추출 (마크다운 코드 블록 제거)
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            JsonNode translationNode = objectMapper.readTree(content);

            TranslationResult result = new TranslationResult();
            result.originalLang = getTextOrNull(translationNode, "original_lang");
            result.titleKo = getTextOrNull(translationNode, "title_ko");
            result.contentKo = getTextOrNull(translationNode, "content_ko");
            result.titleTh = getTextOrNull(translationNode, "title_th");
            result.contentTh = getTextOrNull(translationNode, "content_th");

            return result;
        } catch (Exception e) {
            log.error("Failed to parse translation response: {}", response, e);
            throw new RuntimeException("Failed to parse translation response", e);
        }
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull() || "null".equals(fieldNode.asText())) {
            return null;
        }
        return fieldNode.asText();
    }

    private static class TranslationResult {
        String originalLang;
        String titleKo;
        String contentKo;
        String titleTh;
        String contentTh;
    }
}
