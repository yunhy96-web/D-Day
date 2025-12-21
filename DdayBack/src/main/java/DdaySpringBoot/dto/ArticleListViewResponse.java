package DdaySpringBoot.dto;

import DdaySpringBoot.domain.Article;
import lombok.Getter;

@Getter
public class ArticleListViewResponse {

    private final String uuid;
    private final String title;
    private final String content;

    public ArticleListViewResponse(Article article){
        this.uuid = article.getUuid();
        this.title = article.getTitle();
        this.content = article.getContent();
    }
}
