package DdaySpringBoot.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateTimeUtil {

    private DateTimeUtil() {
    }

    /**
     * UTC 시간을 사용자 타임존으로 변환
     */
    public static LocalDateTime convertToUserTimezone(LocalDateTime utcTime, String timezone) {
        if (utcTime == null || timezone == null) {
            return utcTime;
        }

        ZonedDateTime utcZoned = utcTime.atZone(ZoneId.of("UTC"));
        ZonedDateTime userZoned = utcZoned.withZoneSameInstant(ZoneId.of(timezone));
        return userZoned.toLocalDateTime();
    }

    /**
     * 사용자 타임존 시간을 UTC로 변환
     */
    public static LocalDateTime convertToUtc(LocalDateTime localTime, String timezone) {
        if (localTime == null || timezone == null) {
            return localTime;
        }

        ZonedDateTime userZoned = localTime.atZone(ZoneId.of(timezone));
        ZonedDateTime utcZoned = userZoned.withZoneSameInstant(ZoneId.of("UTC"));
        return utcZoned.toLocalDateTime();
    }
}
