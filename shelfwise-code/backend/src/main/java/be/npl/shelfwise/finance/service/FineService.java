package be.npl.shelfwise.finance.service;

import be.npl.shelfwise.calendar.service.CalendarService;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.common.Money;
import be.npl.shelfwise.config.RulesProperties;
import be.npl.shelfwise.finance.entity.FineRate;
import be.npl.shelfwise.finance.helper.FineCalculator;
import be.npl.shelfwise.finance.helper.FineCalculator.FineResult;
import be.npl.shelfwise.finance.repository.FineRateRepository;
import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.member.entity.MemberCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Looks up the rates that are valid on the calculation date and calculates fines (F-FIN-02, F-FIN-06). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FineService {

    private final FineRateRepository rates;
    private final CalendarService calendar;
    private final RulesProperties rules;

    /**
     * The fine for a loan if it ended on {@code endDate} (return date, lost declaration date, or the business date
     * for the accrued fine of F-FIN-07). The member category is the category on the calculation date.
     */
    public FineResult fineFor(Loan loan, LocalDate endDate) {
        MemberCategory category = loan.getMember() != null ? loan.getMember().getCategory() : loan.getMemberCategory();
        MaterialType material = loan.getCopy().getTitle().getMaterialType();
        BigDecimal rate = dailyRate(category, material, endDate);
        Predicate<LocalDate> closing = calendar.closingDays(loan.getDueDate(), endDate);
        return FineCalculator.calculate(loan.getDueDate(), endDate, category, rate,
                loan.getCopy().effectiveReplacementCost(), closing, rules);
    }

    /** SENIOR members use the ADULT rate, with the discount applied by the calculator. */
    public BigDecimal dailyRate(MemberCategory category, MaterialType material, LocalDate onDate) {
        MemberCategory rateCategory = category == MemberCategory.SENIOR ? MemberCategory.ADULT : category;
        return rates.findFirstByCategoryAndMaterialTypeAndValidFromLessThanEqualOrderByValidFromDesc(
                        rateCategory, material, onDate)
                .map(FineRate::getDailyRate)
                .orElse(Money.ZERO);
    }
}
