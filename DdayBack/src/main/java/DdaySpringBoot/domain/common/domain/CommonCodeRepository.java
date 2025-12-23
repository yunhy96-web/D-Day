package DdaySpringBoot.domain.common.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommonCodeRepository extends JpaRepository<CommonCode, Long> {

    List<CommonCode> findByGroupCodeAndIsActiveTrueOrderBySortOrderAsc(String groupCode);

    Optional<CommonCode> findByGroupCodeAndCode(String groupCode, String code);

    boolean existsByGroupCodeAndCode(String groupCode, String code);
}
