package com.hauly.platform.commoncode.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for CommonCode (group_code, code).
 * Used as @IdClass on CommonCode entity.
 * Domain layer — no Spring/JPA imports (this is a plain serializable class).
 */
public class CommonCodeId implements Serializable {

    private String groupCode;
    private String code;

    public CommonCodeId() {}

    public CommonCodeId(String groupCode, String code) {
        this.groupCode = groupCode;
        this.code = code;
    }

    public String getGroupCode() { return groupCode; }
    public String getCode() { return code; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommonCodeId that)) return false;
        return Objects.equals(groupCode, that.groupCode) && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupCode, code);
    }
}
