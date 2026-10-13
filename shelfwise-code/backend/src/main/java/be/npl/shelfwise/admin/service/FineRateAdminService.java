package be.npl.shelfwise.admin.service;

import be.npl.shelfwise.admin.web.AdminDtos.FineRateRequest;
import be.npl.shelfwise.admin.web.AdminDtos.FineRateView;
import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.common.Money;
import be.npl.shelfwise.common.NotFoundException;
import be.npl.shelfwise.finance.entity.FineRate;
import be.npl.shelfwise.finance.repository.FineRateRepository;
import be.npl.shelfwise.member.entity.MemberCategory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-ADM-01: the daily fine rates with a valid-from date. Past rates cannot be changed. */
@Service
@RequiredArgsConstructor
@Transactional
public class FineRateAdminService {

    private final FineRateRepository rates;
    private final BusinessDateService businessDate;

    @Transactional(readOnly = true)
    public List<FineRateView> list() {
        LocalDate today = businessDate.today();
        return rates.findAllByOrderByValidFromDescCategoryAscMaterialTypeAsc().stream()
                .map(r -> toView(r, today)).toList();
    }

    public FineRateView create(FineRateRequest request) {
        LocalDate today = businessDate.today();
        if (request.category() == MemberCategory.SENIOR) {
            throw new BusinessRuleException("SENIOR_RATE_DERIVED", "BR-FIN-02",
                    "SENIOR members pay the ADULT rate with a 50% discount; maintain the ADULT rate instead.");
        }
        if (request.dailyRate().signum() < 0) {
            throw new BusinessRuleException("INVALID_RATE", "F-ADM-01", "A rate cannot be negative.");
        }
        if (request.validFrom().isBefore(today)) {
            throw new BusinessRuleException("PAST_RATE", "F-ADM-01", "A rate cannot start in the past.");
        }
        if (rates.existsByCategoryAndMaterialTypeAndValidFrom(request.category(), request.materialType(), request.validFrom())) {
            throw new BusinessRuleException("DUPLICATE_RATE", "F-ADM-01",
                    "There is already a rate for this category and material type from that date.");
        }
        FineRate rate = new FineRate();
        rate.setCategory(request.category());
        rate.setMaterialType(request.materialType());
        rate.setDailyRate(Money.round(request.dailyRate()));
        rate.setValidFrom(request.validFrom());
        return toView(rates.save(rate), today);
    }

    /** Only rates that have not started yet can be removed. */
    public void delete(Long id) {
        FineRate rate = rates.findById(id).orElseThrow(() -> new NotFoundException("Fine rate", id));
        if (!rate.getValidFrom().isAfter(businessDate.today())) {
            throw new BusinessRuleException("PAST_RATE", "F-ADM-01", "A rate that is already valid cannot be changed.");
        }
        rates.delete(rate);
    }

    private FineRateView toView(FineRate r, LocalDate today) {
        return new FineRateView(r.getId(), r.getCategory(), r.getMaterialType(), r.getDailyRate(), r.getValidFrom(),
                r.getValidFrom().isAfter(today));
    }
}
