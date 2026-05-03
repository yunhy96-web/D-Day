package com.hauly.platform.commoncode.application.command;

/**
 * Command for creating a new common code.
 */
public record CreateCodeCommand(
        String groupCode,
        String code,
        String nameKo,
        String nameEn,
        String nameTh,
        int sortOrder
) {}
