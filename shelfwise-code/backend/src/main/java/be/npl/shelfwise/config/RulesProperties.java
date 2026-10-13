package be.npl.shelfwise.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The fixed business parameters of section 4 of the specification, kept in one place.
 * Parameters that differ per category or material type live on the enums MemberCategory and MaterialType.
 * Admin-maintained parameters (fine rates, holidays, loan limits) live in the database.
 */
@ConfigurationProperties("shelfwise.rules")
public record RulesProperties(
        @DefaultValue("2") int maxRenewals,
        @DefaultValue("5") int maxActiveReservations,
        @DefaultValue("7") int holdPeriodDays,
        @DefaultValue("5") int pickupReminderDays,
        @DefaultValue("50") int seniorDiscountPercent,
        @DefaultValue("1") int graceChargeableDays,
        @DefaultValue("10.00") BigDecimal maxFinePerCopy,
        @DefaultValue("42") int lostAfterDays,
        @DefaultValue("5.00") BigDecimal lostAdminFee,
        @DefaultValue("90") int lostRefundWindowDays,
        @DefaultValue("5.00") BigDecimal blockingThreshold,
        @DefaultValue("2.00") BigDecimal librarianWaiverLimit,
        @DefaultValue("12") int membershipMonths,
        @DefaultValue("12") int anonymiseAfterMonths,
        @DefaultValue("2") int dueReminderDaysBefore,
        @DefaultValue("1") int firstOverdueNoticeDays,
        @DefaultValue("7") int secondOverdueNoticeDays,
        @DefaultValue("21") int thirdOverdueNoticeDays,
        @DefaultValue("30") int membershipReminderDays) {

    /** The values of section 4, for code that runs without a Spring context (unit tests). */
    public static RulesProperties defaults() {
        return new RulesProperties(2, 5, 7, 5, 50, 1, new BigDecimal("10.00"), 42, new BigDecimal("5.00"), 90,
                new BigDecimal("5.00"), new BigDecimal("2.00"), 12, 12, 2, 1, 7, 21, 30);
    }
}
