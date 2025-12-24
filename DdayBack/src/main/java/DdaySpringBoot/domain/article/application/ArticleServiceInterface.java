package DdaySpringBoot.domain.article.application;

import DdaySpringBoot.domain.article.domain.Article;
import DdaySpringBoot.domain.article.dto.AddArticleRequest;
import DdaySpringBoot.domain.article.dto.UpdateArticleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ArticleServiceInterface {

    Article save(AddArticleRequest request, Long userNo, String userRole);

    List<Article> findAll();

    List<Article> findByTopic(String topic);

    List<Article> findByArticleType(String articleType);

    List<Article> findByArticleTypeAndTopic(String articleType, String topic);

    Article findByUuid(String uuid);

    void delete(String uuid, Long userNo, String userRole);

    Article update(String uuid, UpdateArticleRequest request, Long userNo, String userRole);

    // 권한 기반 조회
    List<Article> findAllByRole(String userRole);

    List<Article> findByTopicAndRole(String topic, String userRole);

    // 페이지네이션 지원 메서드
    Page<Article> findAllByRolePaged(String userRole, Pageable pageable);

    Page<Article> findByTopicAndRolePaged(String topic, String userRole, Pageable pageable);

    Page<Article> findByArticleTypePaged(String articleType, Pageable pageable);

    Page<Article> findByArticleTypeAndTopicPaged(String articleType, String topic, Pageable pageable);

    // 검색 지원 메서드
    Page<Article> searchByKeyword(String userRole, String articleType, String topic, String keyword, Pageable pageable);
}
