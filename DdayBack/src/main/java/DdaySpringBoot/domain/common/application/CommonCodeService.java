package DdaySpringBoot.domain.common.application;

import DdaySpringBoot.domain.common.domain.CommonCode;
import DdaySpringBoot.domain.common.domain.CommonCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommonCodeService {

    private final CommonCodeRepository commonCodeRepository;

    public List<CommonCode> getCodesByGroup(String groupCode) {
        return commonCodeRepository.findByGroupCodeAndIsActiveTrueOrderBySortOrderAsc(groupCode);
    }

    public boolean isValidCode(String groupCode, String code) {
        return commonCodeRepository.existsByGroupCodeAndCode(groupCode, code);
    }
}
