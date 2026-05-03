package com.hauly.platform.commoncode.application.query;

import com.hauly.platform.commoncode.domain.model.CommonCode;
import com.hauly.shared.kernel.Lang;

/**
 * Read model for a single common code.
 */
public record CommonCodeView(
        String groupCode,
        String code,
        String name,
        int sortOrder,
        String attributes,
        boolean active
) {
    public static CommonCodeView from(CommonCode cc, Lang lang) {
        return new CommonCodeView(
                cc.getGroupCode(),
                cc.getCode(),
                cc.pickName(lang),
                cc.getSortOrder(),
                cc.getAttributes(),
                cc.isActive()
        );
    }
}
