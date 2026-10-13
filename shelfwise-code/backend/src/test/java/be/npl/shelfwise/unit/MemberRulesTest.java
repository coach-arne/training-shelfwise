package be.npl.shelfwise.unit;

import static org.assertj.core.api.Assertions.assertThat;

import be.npl.shelfwise.finance.helper.PaymentAllocator;
import be.npl.shelfwise.finance.helper.PaymentAllocator.Allocation;
import be.npl.shelfwise.finance.helper.PaymentAllocator.ChargeOutstanding;
import be.npl.shelfwise.lending.helper.CopyRouter;
import be.npl.shelfwise.lending.helper.CopyRouter.RoutingOutcome;
import be.npl.shelfwise.member.entity.MemberCategory;
import be.npl.shelfwise.member.helper.CategoryResolver;
import be.npl.shelfwise.member.helper.MembershipPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MemberRulesTest {

    @ParameterizedTest(name = "{0}")
    @DisplayName("F-MEM-01: category from age on the registration date or membership renewal date (AC-MEM-07)")
    @CsvSource({
            "2008-06-15, 2026-03-02, YOUTH",
            "2008-06-15, 2027-03-02, ADULT",
            "2008-03-02, 2026-03-02, ADULT",
            "2008-03-03, 2026-03-02, YOUTH",
            "1961-03-02, 2026-03-01, ADULT",
            "1961-03-02, 2026-03-02, SENIOR",
    })
    void category(LocalDate birth, LocalDate on, MemberCategory expected) {
        assertThat(CategoryResolver.resolve(birth, on)).isEqualTo(expected);
    }

    @Test
    @DisplayName("F-MEM-04: a membership lasts 12 months; the last valid day is inclusive")
    void membershipPeriod() {
        var period = MembershipPeriod.startingOn(LocalDate.of(2026, 3, 2), 12);
        assertThat(period.end()).isEqualTo(LocalDate.of(2027, 3, 1));
        assertThat(MembershipPeriod.isValidOn(period.end(), LocalDate.of(2027, 3, 1))).isTrue();
        assertThat(MembershipPeriod.isValidOn(period.end(), LocalDate.of(2027, 3, 2))).isFalse();
    }

    @Test
    @DisplayName("F-MEM-04: an early renewal extends from the day after the current end; a late one starts today")
    void renewalPeriod() {
        var early = MembershipPeriod.renewal(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1), 12);
        assertThat(early.start()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(early.end()).isEqualTo(LocalDate.of(2027, 9, 30));
        var late = MembershipPeriod.renewal(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 16), 12);
        assertThat(late.start()).isEqualTo(LocalDate.of(2026, 3, 16));
        assertThat(late.end()).isEqualTo(LocalDate.of(2027, 3, 15));
    }

    @Test
    @DisplayName("AC-PAY-01: payments are allocated to the oldest charge first")
    void allocation() {
        List<Allocation> result = PaymentAllocator.allocateOldestFirst(List.of(
                new ChargeOutstanding(1L, new BigDecimal("1.00")),
                new ChargeOutstanding(2L, new BigDecimal("3.00")),
                new ChargeOutstanding(3L, new BigDecimal("2.00"))), new BigDecimal("3.50"));
        assertThat(result).hasSize(2);
        assertThat(result.get(0).chargeId()).isEqualTo(1L);
        assertThat(result.get(0).amount()).isEqualByComparingTo("1.00");
        assertThat(result.get(1).chargeId()).isEqualTo(2L);
        assertThat(result.get(1).amount()).isEqualByComparingTo("2.50");
    }

    @Test
    @DisplayName("F-LEN-06 routing table (AC-LEN-12, AC-LEN-13)")
    void routing() {
        // returned at CEN (1), home CEN, nobody waiting
        assertThat(CopyRouter.decide(1L, 1L, null, true)).isEqualTo(RoutingOutcome.AVAILABLE_HERE);
        // returned at RIV (2), home CEN, nobody waiting
        assertThat(CopyRouter.decide(2L, 1L, null, true)).isEqualTo(RoutingOutcome.TRANSIT_TO_HOME);
        // first waiting reservation picks up at the branch where the copy is
        assertThat(CopyRouter.decide(1L, 3L, 1L, true)).isEqualTo(RoutingOutcome.HOLD_HERE);
        // first waiting reservation picks up elsewhere
        assertThat(CopyRouter.decide(1L, 1L, 2L, true)).isEqualTo(RoutingOutcome.TRANSIT_TO_PICKUP);
        // copies that stay where they are when nothing is waiting
        assertThat(CopyRouter.decide(2L, 1L, null, false)).isEqualTo(RoutingOutcome.AVAILABLE_HERE);
    }
}
