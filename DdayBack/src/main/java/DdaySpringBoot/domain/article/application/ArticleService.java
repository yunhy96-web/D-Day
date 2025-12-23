package DdaySpringBoot.domain.article.application;

import DdaySpringBoot.domain.article.application.event.ArticleCreatedEvent;
import DdaySpringBoot.domain.article.domain.Article;
import DdaySpringBoot.domain.article.domain.ArticleRepository;
import DdaySpringBoot.domain.article.dto.AddArticleRequest;
import DdaySpringBoot.domain.article.dto.UpdateArticleRequest;
import DdaySpringBoot.domain.permission.application.PermissionService;
import DdaySpringBoot.global.exception.AuthException;
import DdaySpringBoot.global.exception.EntityNotFoundException;
import DdaySpringBoot.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService implements ArticleServiceInterface {

    private final ArticleRepository articleRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PermissionService permissionService;

    @Override
    @Transactional
    public Article save(AddArticleRequest request, Long userNo, String userRole) {
        // 해당 타입에 쓰기 권한이 있는지 확인
        if (!permissionService.canWrite(userRole, request.getArticleType())) {
            throw new AuthException(ErrorCode.ACCESS_DENIED, "해당 게시글 타입에 대한 작성 권한이 없습니다");
        }
        Article article = articleRepository.save(request.toEntity(userNo));
        eventPublisher.publishEvent(new ArticleCreatedEvent(article.getNo()));
        return article;
    }

    @Override
    public List<Article> findAll() {
        return articleRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<Article> findByTopic(String topic) {
        return articleRepository.findByTopicOrderByCreatedAtDesc(topic);
    }

    @Override
    public List<Article> findByArticleType(String articleType) {
        return articleRepository.findByArticleTypeOrderByCreatedAtDesc(articleType);
    }

    @Override
    public List<Article> findByArticleTypeAndTopic(String articleType, String topic) {
        return articleRepository.findByArticleTypeAndTopicOrderByCreatedAtDesc(articleType, topic);
    }

    @Override
    public Article findByUuid(String uuid) {
        return articleRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ARTICLE_NOT_FOUND));
    }

    @Override
    @Transactional
    public void delete(String uuid, Long userNo, String userRole) {
        Article article = findByUuid(uuid);
        validateOwnership(article, userNo, userRole);
        articleRepository.delete(article);
    }

    @Override
    @Transactional
    public Article update(String uuid, UpdateArticleRequest request, Long userNo, String userRole) {
        Article article = findByUuid(uuid);
        validateOwnership(article, userNo, userRole);
        article.update(request.getTopic(), request.getArticleType(), request.getTitle(), request.getContent(), userNo);
        articleRepository.save(article);
        eventPublisher.publishEvent(new ArticleCreatedEvent(article.getNo()));
        return article;
    }

    private void validateOwnership(Article article, Long userNo, String userRole) {
        // DEV, ADMIN은 모든 글 수정/삭제 가능
        if ("DEV".equals(userRole) || "ADMIN".equals(userRole)) {
            return;
        }
        // 일반 USER는 본인 글만 수정/삭제 가능
        if (!userNo.equals(article.getCreatedBy())) {
            throw new AuthException(ErrorCode.ACCESS_DENIED, "본인이 작성한 글만 수정/삭제할 수 있습니다");
        }
    }

    @Override
    public List<Article> findAllByRole(String userRole) {
        List<String> readableTypes = permissionService.getReadableArticleTypes(userRole);
        if (readableTypes.isEmpty()) {
            return List.of();
        }
        return articleRepository.findByArticleTypeInOrderByCreatedAtDesc(readableTypes);
    }

    @Override
    public List<Article> findByTopicAndRole(String topic, String userRole) {
        List<String> readableTypes = permissionService.getReadableArticleTypes(userRole);
        if (readableTypes.isEmpty()) {
            return List.of();
        }
        return articleRepository.findByArticleTypeInAndTopicOrderByCreatedAtDesc(readableTypes, topic);
    }
}
