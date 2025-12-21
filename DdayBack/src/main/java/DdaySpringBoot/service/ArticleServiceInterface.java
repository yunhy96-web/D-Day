package DdaySpringBoot.service;

import DdaySpringBoot.domain.Article;
import DdaySpringBoot.dto.AddArticleRequest;
import DdaySpringBoot.dto.UpdateArticleRequest;

import java.util.List;

public interface ArticleServiceInterface {

    Article save(AddArticleRequest request);

    List<Article> findAll();

    Article findByUuid(String uuid);

    void delete(String uuid);

    Article update(String uuid, UpdateArticleRequest request);
}
