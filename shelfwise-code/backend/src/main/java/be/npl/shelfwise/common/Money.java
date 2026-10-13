package be.npl.shelfwise.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Helper functions for euro amounts. All amounts have scale 2. */
public final class Money {

    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {
    }

    public static BigDecimal of(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    /** Rounds to whole cents, half-up (BR-FIN-05). */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    public static boolean gt(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) > 0;
    }

    public static boolean gte(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0;
    }
}
