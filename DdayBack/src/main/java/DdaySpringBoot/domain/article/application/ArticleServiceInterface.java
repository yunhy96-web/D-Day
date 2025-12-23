package DdaySpringBoot.domain.article.application;

import DdaySpringBoot.domain.article.domain.Article;
import DdaySpringBoot.domain.article.dto.AddArticleRequest;
import DdaySpringBoot.domain.article.dto.UpdateArticleRequest;

import java.util.List;

public interface ArticleServiceInterface {

    Article save(AddArticleRequest request, Long userNo);

    List<Article> findAll();

    List<Article> findByTopic(String topic);

    List<Article> findByArticleType(String articleType);

    List<Article> findByArticleTypeAndTopic(String articleType, String topic);

    Article findByUuid(String uuid);

    void delete(String uuid, Long userNo, String userRole);

    Article update(String uuid, UpdateArticleRequest request, Long userNo, String userRole);
}
