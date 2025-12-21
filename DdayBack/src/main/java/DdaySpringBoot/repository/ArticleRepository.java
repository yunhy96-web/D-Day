package DdaySpringBoot.repository;

import DdaySpringBoot.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findByUuid(String uuid);
}
