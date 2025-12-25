package DdaySpringBoot.domain.crawler.application;

import DdaySpringBoot.domain.article.domain.Article;
import DdaySpringBoot.domain.article.domain.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {

    private final ArticleRepository articleRepository;

    private static final String SOURCE_PREFIX = "hotssul_";
    private static final String LIST_URL = "https://hotssul.com/bbs/board.php?bo_table=ssul19&page=";
    private static final String DETAIL_URL = "https://hotssul.com/bbs/board.php?bo_table=ssul19&wr_id=";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final int ARTICLES_PER_PAGE = 25;

    /**
     * 특정 페이지의 글을 크롤링하여 저장
     * @param page 크롤링할 페이지 번호
     * @return 저장된 글 개수
     */
    @Transactional
    public int crawlAndSavePage(int page) {
        log.info("크롤링 시작 - 페이지: {}", page);

        List<String> articleIds = getArticleIdsFromPage(page);
        log.info("페이지 {}에서 {}개 글 발견", page, articleIds.size());

        int savedCount = 0;

        for (String articleId : articleIds) {
            try {
                String sourceId = SOURCE_PREFIX + articleId;

                // articles 테이블에서 source_id로 중복 체크
                if (articleRepository.existsBySourceId(sourceId)) {
                    log.debug("이미 크롤링된 글: {}", articleId);
                    continue;
                }

                ArticleData data = crawlArticleDetail(articleId);
                if (data != null) {
                    saveArticle(data, sourceId);
                    savedCount++;
                    log.info("글 저장 완료: {} - {}", articleId, data.title);
                }

                // 서버 부하 방지를 위한 딜레이
                Thread.sleep(300);
            } catch (Exception e) {
                log.error("글 크롤링 실패: {} - {}", articleId, e.getMessage());
            }
        }

        log.info("크롤링 완료 - 페이지: {}, 저장된 글: {}개", page, savedCount);
        return savedCount;
    }

    /**
     * 특정 페이지에서 글 ID 목록 추출
     * div.list-board > ul.list-body > li.list-item (bg-light 제외) 내부의 링크에서 wr_id 추출
     */
    private List<String> getArticleIdsFromPage(int page) {
        List<String> ids = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(LIST_URL + page)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();

            // 명시적 셀렉터: li.list-item만 선택 (bg-light 클래스가 있는 것은 제외)
            Elements listItems = doc.select("div.list-board ul.list-body li.list-item:not(.bg-light)");
            log.debug("li.list-item 개수 (bg-light 제외): {}", listItems.size());

            Pattern pattern = Pattern.compile("wr_id=(\\d+)");

            for (Element item : listItems) {
                // 각 list-item 내부의 링크에서 wr_id 추출
                Element link = item.selectFirst("a[href*=wr_id=]");
                if (link != null) {
                    String href = link.attr("href");
                    Matcher matcher = pattern.matcher(href);
                    if (matcher.find()) {
                        String id = matcher.group(1);
                        if (!ids.contains(id)) {
                            ids.add(id);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("목록 페이지 크롤링 실패 (page={}): {}", page, e.getMessage());
        }

        return ids;
    }

    private ArticleData crawlArticleDetail(String articleId) {
        try {
            Document doc = Jsoup.connect(DETAIL_URL + articleId)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();

            // 제목 추출
            String title = doc.select("meta[property=og:title]").attr("content");
            if (title.isEmpty()) {
                title = doc.select("meta[name=title]").attr("content");
            }

            // 본문 추출
            Element contentDiv = doc.selectFirst(".view-content2");
            if (contentDiv == null) {
                contentDiv = doc.selectFirst(".view-content");
            }

            String content = "";
            if (contentDiv != null) {
                // 출처 정보 영역 제거 (hotssul3 등)
                contentDiv.select(".hotssul3").remove();
                contentDiv.select(".hotssul2").remove();
                contentDiv.select(".hotssul1").remove();

                // <br> 태그를 줄바꿈으로 변환
                contentDiv.select("br").after("\\n");
                // <p> 태그 끝을 줄바꿈으로 변환
                contentDiv.select("p").after("\\n");

                // HTML 태그 제거하고 텍스트만 추출
                content = contentDiv.text()
                        .replace("\\n", "\n")  // 임시 마커를 실제 줄바꿈으로
                        .replaceAll("\n{3,}", "\n\n")  // 연속 3개 이상 줄바꿈은 2개로
                        .trim();
            }

            if (title.isEmpty() || content.isEmpty()) {
                log.warn("제목 또는 내용이 비어있음: {}", articleId);
                return null;
            }

            return new ArticleData(title, content);
        } catch (IOException e) {
            log.error("상세 페이지 크롤링 실패: {} - {}", articleId, e.getMessage());
            return null;
        }
    }

    private void saveArticle(ArticleData data, String sourceId) {
        Article article = Article.builder()
                .topic("ADULT")
                .articleType("CRAWLED")
                .title(data.title)
                .content(data.content)
                .createdBy(null)
                .sourceId(sourceId)
                .build();

        articleRepository.save(article);
    }

    private record ArticleData(String title, String content) {}
}
