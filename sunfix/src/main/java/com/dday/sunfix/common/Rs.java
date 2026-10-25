package com.dday.sunfix.common;

import java.sql.ResultSet;
import java.sql.SQLException;

/** SQLite 는 작은 정수를 Integer 로 준다. NULL 을 살리면서 타입을 고정 */
public final class Rs {
    private Rs() {}
    public static Long lng(ResultSet rs, int col) throws SQLException { long v = rs.getLong(col); return rs.wasNull() ? null : v; }
    public static Integer integer(ResultSet rs, int col) throws SQLException { int v = rs.getInt(col); return rs.wasNull() ? null : v; }
}
