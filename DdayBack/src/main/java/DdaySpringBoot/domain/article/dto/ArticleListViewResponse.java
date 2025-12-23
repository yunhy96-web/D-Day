package DdaySpringBoot.domain.article.dto;

import DdaySpringBoot.domain.article.domain.Article;
import lombok.Getter;

@Getter
public class ArticleListViewResponse {

    private final String uuid;
    private final String title;
    private final String content;
    private final String originalLang;
    private final String titleKo;
    private final String contentKo;
    private final String titleTh;
    private final String contentTh;
    private final String translationStatus;
    private final Long createdBy;
    private final Long updatedBy;

    public ArticleListViewResponse(Article article){
        this.uuid = article.getUuid();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.originalLang = article.getOriginalLang();
        this.titleKo = article.getTitleKo();
        this.contentKo = article.getContentKo();
        this.titleTh = article.getTitleTh();
        this.contentTh = article.getContentTh();
        this.translationStatus = article.getTranslationStatus();
        this.createdBy = article.getCreatedBy();
        this.updatedBy = article.getUpdatedBy();
    }
}
