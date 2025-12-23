package DdaySpringBoot.domain.article.dto;

import DdaySpringBoot.domain.article.domain.Article;
import DdaySpringBoot.global.util.DateTimeUtil;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ArticleResponse {

    private final String uuid;
    private final String topic;
    private final String articleType;
    private final String title;
    private final String content;
    private final String originalLang;
    private final String titleKo;
    private final String contentKo;
    private final String titleTh;
    private final String contentTh;
    private final String translationStatus;
    private final String authorNickname;
    private final Long createdBy;
    private final Long updatedBy;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ArticleResponse(Article article, String authorNickname) {
        this.uuid = article.getUuid();
        this.topic = article.getTopic();
        this.articleType = article.getArticleType();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.originalLang = article.getOriginalLang();
        this.titleKo = article.getTitleKo();
        this.contentKo = article.getContentKo();
        this.titleTh = article.getTitleTh();
        this.contentTh = article.getContentTh();
        this.translationStatus = article.getTranslationStatus();
        this.authorNickname = authorNickname;
        this.createdBy = article.getCreatedBy();
        this.updatedBy = article.getUpdatedBy();
        this.createdAt = article.getCreatedAt();
        this.updatedAt = article.getUpdatedAt();
    }

    public ArticleResponse(Article article, String authorNickname, String timezone) {
        this.uuid = article.getUuid();
        this.topic = article.getTopic();
        this.articleType = article.getArticleType();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.originalLang = article.getOriginalLang();
        this.titleKo = article.getTitleKo();
        this.contentKo = article.getContentKo();
        this.titleTh = article.getTitleTh();
        this.contentTh = article.getContentTh();
        this.translationStatus = article.getTranslationStatus();
        this.authorNickname = authorNickname;
        this.createdBy = article.getCreatedBy();
        this.updatedBy = article.getUpdatedBy();
        this.createdAt = DateTimeUtil.convertToUserTimezone(article.getCreatedAt(), timezone);
        this.updatedAt = DateTimeUtil.convertToUserTimezone(article.getUpdatedAt(), timezone);
    }
}
