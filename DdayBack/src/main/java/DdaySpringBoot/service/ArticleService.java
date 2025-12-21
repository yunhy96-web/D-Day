package DdaySpringBoot.service;

import DdaySpringBoot.common.exception.EntityNotFoundException;
import DdaySpringBoot.common.exception.ErrorCode;
import DdaySpringBoot.domain.Article;
import DdaySpringBoot.dto.AddArticleRequest;
import DdaySpringBoot.dto.UpdateArticleRequest;
import DdaySpringBoot.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService implements ArticleServiceInterface {

    private final ArticleRepository articleRepository;

    @Override
    @Transactional
    public Article save(AddArticleRequest request) {
        return articleRepository.save(request.toEntity());
    }

    @Override
    public List<Article> findAll() {
        return articleRepository.findAll();
    }

    @Override
    public Article findByUuid(String uuid) {
        return articleRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ARTICLE_NOT_FOUND));
    }

    @Override
    @Transactional
    public void delete(String uuid) {
        Article article = findByUuid(uuid);
        articleRepository.delete(article);
    }

    @Override
    @Transactional
    public Article update(String uuid, UpdateArticleRequest request) {
        Article article = findByUuid(uuid);
        article.update(request.getTitle(), request.getContent());
        return article;
    }
}
