package be.npl.shelfwise.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public final class TimeHelper {

    private TimeHelper() {
    }

    /**
     * The moment at which something happens on the given business date. In production this is simply "now"; in
     * training mode the date part follows the business date so that queue order stays consistent.
     */
    public static Instant onBusinessDate(LocalDate businessDate, ZoneId zone) {
        return businessDate.atTime(LocalTime.now(zone)).atZone(zone).toInstant();
    }
}
