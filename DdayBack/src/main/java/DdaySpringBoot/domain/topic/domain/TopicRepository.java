package DdaySpringBoot.domain.topic.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByIsActiveTrueOrderBySortOrderAsc();

    Optional<Topic> findByCode(String code);

    boolean existsByCode(String code);
}
