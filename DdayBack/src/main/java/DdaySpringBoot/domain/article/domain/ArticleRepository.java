package DdaySpringBoot.domain.article.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    // 삭제되지 않은 게시글만 조회
    Optional<Article> findByUuidAndIsDeletedFalse(String uuid);

    List<Article> findByTopicAndIsDeletedFalseOrderByCreatedAtDesc(String topic);

    List<Article> findByArticleTypeAndIsDeletedFalseOrderByCreatedAtDesc(String articleType);

    List<Article> findByArticleTypeAndTopicAndIsDeletedFalseOrderByCreatedAtDesc(String articleType, String topic);

    List<Article> findByIsDeletedFalseOrderByCreatedAtDesc();

    // 권한 기반 조회용 (삭제되지 않은 것만)
    List<Article> findByArticleTypeInAndIsDeletedFalseOrderByCreatedAtDesc(List<String> articleTypes);

    List<Article> findByArticleTypeInAndTopicAndIsDeletedFalseOrderByCreatedAtDesc(List<String> articleTypes, String topic);

    // 페이지네이션 지원 메서드
    Page<Article> findByArticleTypeInAndIsDeletedFalse(List<String> articleTypes, Pageable pageable);

    Page<Article> findByArticleTypeInAndTopicAndIsDeletedFalse(List<String> articleTypes, String topic, Pageable pageable);

    Page<Article> findByArticleTypeAndIsDeletedFalse(String articleType, Pageable pageable);

    Page<Article> findByArticleTypeAndTopicAndIsDeletedFalse(String articleType, String topic, Pageable pageable);

    // 검색 지원 메서드 (LIKE 검색)
    Page<Article> findByArticleTypeInAndTitleContainingAndIsDeletedFalse(List<String> articleTypes, String keyword, Pageable pageable);

    Page<Article> findByArticleTypeInAndTopicAndTitleContainingAndIsDeletedFalse(List<String> articleTypes, String topic, String keyword, Pageable pageable);

    Page<Article> findByArticleTypeAndTitleContainingAndIsDeletedFalse(String articleType, String keyword, Pageable pageable);

    Page<Article> findByArticleTypeAndTopicAndTitleContainingAndIsDeletedFalse(String articleType, String topic, String keyword, Pageable pageable);

    // 크롤링 중복 체크용 (삭제 여부 상관없이 source_id만 체크)
    boolean existsBySourceId(String sourceId);
}
