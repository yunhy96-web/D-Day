package com.hauly.platform.commoncode.application.command;

/**
 * Command for updating a common code.
 */
public record UpdateCodeCommand(
        String groupCode,
        String code,
        String nameKo,
        String nameEn,
        String nameTh,
        Integer sortOrder,
        Boolean active
) {}
