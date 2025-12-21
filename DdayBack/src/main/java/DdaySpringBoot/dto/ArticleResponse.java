package DdaySpringBoot.dto;

import DdaySpringBoot.common.util.DateTimeUtil;
import DdaySpringBoot.domain.Article;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ArticleResponse {

    private final String uuid;
    private final String title;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ArticleResponse(Article article) {
        this.uuid = article.getUuid();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.createdAt = article.getCreatedAt();
        this.updatedAt = article.getUpdatedAt();
    }

    public ArticleResponse(Article article, String timezone) {
        this.uuid = article.getUuid();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.createdAt = DateTimeUtil.convertToUserTimezone(article.getCreatedAt(), timezone);
        this.updatedAt = DateTimeUtil.convertToUserTimezone(article.getUpdatedAt(), timezone);
    }
}
