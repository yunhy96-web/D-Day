package com.hauly.platform.commoncode.application.query;

import com.hauly.platform.commoncode.domain.model.CommonCodeGroup;

/**
 * Read model for a code group.
 */
public record CommonCodeGroupView(
        String groupCode,
        String groupNameKo,
        String groupNameEn,
        String description,
        boolean system,
        boolean active
) {
    public static CommonCodeGroupView from(CommonCodeGroup g) {
        return new CommonCodeGroupView(
                g.getGroupCode(),
                g.getGroupNameKo(),
                g.getGroupNameEn(),
                g.getDescription(),
                g.isSystem(),
                g.isActive()
        );
    }
}
