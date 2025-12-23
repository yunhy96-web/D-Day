package DdaySpringBoot.domain.common.dto;

import DdaySpringBoot.domain.common.domain.CommonCode;
import lombok.Getter;

@Getter
public class CommonCodeResponse {

    private final String code;
    private final String label;
    private final String labelKo;
    private final String labelEn;
    private final String labelTh;

    public CommonCodeResponse(CommonCode commonCode, String lang) {
        this.code = commonCode.getCode();
        this.label = commonCode.getLabelByLang(lang);
        this.labelKo = commonCode.getLabelKo();
        this.labelEn = commonCode.getLabelEn();
        this.labelTh = commonCode.getLabelTh();
    }
}
