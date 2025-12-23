package DdaySpringBoot.domain.article.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findByUuid(String uuid);

    List<Article> findByTopicOrderByCreatedAtDesc(String topic);

    List<Article> findByArticleTypeOrderByCreatedAtDesc(String articleType);

    List<Article> findByArticleTypeAndTopicOrderByCreatedAtDesc(String articleType, String topic);

    List<Article> findAllByOrderByCreatedAtDesc();
}
