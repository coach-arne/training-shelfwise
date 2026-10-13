package be.npl.shelfwise.finance.helper;

import be.npl.shelfwise.calendar.helper.CalendarRules;
import be.npl.shelfwise.common.Money;
import be.npl.shelfwise.config.RulesProperties;
import be.npl.shelfwise.member.entity.MemberCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Predicate;

/** The overdue fine in the order of calculation of F-FIN-06. */
public final class FineCalculator {

    public record FineResult(int chargeableDays, BigDecimal fine) {
    }

    private FineCalculator() {
    }

    /**
     * @param dailyRate       the rate per chargeable day before any discount; for SENIOR members this is the ADULT rate
     * @param replacementCost the effective replacement cost of the copy (BR-FIN-06)
     * @param closingDay      closing days between the due date and the end date
     */
    public static FineResult calculate(LocalDate dueDate, LocalDate endDate, MemberCategory category,
                                       BigDecimal dailyRate, BigDecimal replacementCost,
                                       Predicate<LocalDate> closingDay, RulesProperties rules) {
        // 1. Count the chargeable days (BR-FIN-01).
        int chargeableDays = endDate.isAfter(dueDate) ? CalendarRules.countOpeningDays(dueDate, endDate, closingDay) : 0;
        // 2. Grace period (BR-FIN-03): at most 1 chargeable day means no fine.
        if (chargeableDays <= rules.graceChargeableDays()) {
            return new FineResult(chargeableDays, Money.ZERO);
        }
        // 3. Multiply by the daily rate (BR-FIN-02, BR-FIN-11).
        BigDecimal fine = dailyRate.multiply(BigDecimal.valueOf(chargeableDays));
        // 4. Senior discount (BR-FIN-04).
        if (category == MemberCategory.SENIOR) {
            fine = fine.multiply(BigDecimal.valueOf(100 - rules.seniorDiscountPercent())).divide(BigDecimal.valueOf(100));
        }
        // 5. Round half-up to cents, once per copy (BR-FIN-05).
        fine = Money.round(fine);
        // 6. Maximum: the lower of the configured maximum and the replacement cost (BR-FIN-06).
        fine = Money.min(fine, Money.min(rules.maxFinePerCopy(), Money.round(replacementCost)));
        return new FineResult(chargeableDays, fine);
    }
}
