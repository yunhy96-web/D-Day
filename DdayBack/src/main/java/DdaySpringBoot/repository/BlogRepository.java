package DdaySpringBoot.repository;

import DdaySpringBoot.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Article, Long> {
}
