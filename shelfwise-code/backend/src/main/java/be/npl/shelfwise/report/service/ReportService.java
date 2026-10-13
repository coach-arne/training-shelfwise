package be.npl.shelfwise.report.service;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.finance.helper.FineCalculator.FineResult;
import be.npl.shelfwise.finance.service.FineService;
import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.lending.entity.LoanEvent;
import be.npl.shelfwise.lending.entity.LoanStatus;
import be.npl.shelfwise.lending.repository.LoanEventRepository;
import be.npl.shelfwise.lending.repository.LoanRepository;
import be.npl.shelfwise.report.helper.CsvWriter;
import be.npl.shelfwise.security.CurrentUser;
import be.npl.shelfwise.security.Role;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operational reports R-01 (daily circulation) and R-02 (overdue items) with CSV export (F-REP-01). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    public static final String ONLINE = "ONLINE";

    private final LoanEventRepository events;
    private final LoanRepository loans;
    private final FineService fines;
    private final BusinessDateService businessDate;

    public record CirculationRow(String branch, MaterialType materialType, long checkouts, long returns, long renewals) {
    }

    public record OverdueRow(Long loanId, String memberName, String cardNumber, String barcode, String title,
                             MaterialType materialType, LocalDate dueDate, long daysOverdue, int chargeableDays,
                             BigDecimal accruedFine, String branch) {
    }

    /**
     * Librarians see their own branch only (AC-ROL-01); branch managers and administrators see all branches, or one
     * branch when requested. Returns null for "all branches".
     */
    public Long resolveBranch(CurrentUser user, Long requestedBranchId) {
        if (user.role() == Role.LIBRARIAN) {
            if (requestedBranchId != null && !requestedBranchId.equals(user.branchId())) {
                throw new AccessDeniedException("A librarian can only see the reports of their own branch");
            }
            return user.branchId();
        }
        return requestedBranchId;
    }

    // ----- R-01 -----

    public List<CirculationRow> dailyCirculation(LocalDate date, Long branchId) {
        Map<String, long[]> counters = new TreeMap<>();
        for (LoanEvent event : events.findByEventDateWithBranch(date)) {
            String branch = event.getBranch() == null ? ONLINE : event.getBranch().getCode();
            if (branchId != null && (event.getBranch() == null || !event.getBranch().getId().equals(branchId))) {
                continue;
            }
            long[] c = counters.computeIfAbsent(branch + "|" + event.getMaterialType().name(), k -> new long[3]);
            switch (event.getType()) {
                case CHECKOUT -> c[0]++;
                case RETURN -> c[1]++;
                case RENEWAL -> c[2]++;
            }
        }
        return counters.entrySet().stream().map(e -> {
            String[] key = e.getKey().split("\\|");
            return new CirculationRow(key[0], MaterialType.valueOf(key[1]), e.getValue()[0], e.getValue()[1], e.getValue()[2]);
        }).toList();
    }

    public String circulationCsv(List<CirculationRow> rows) {
        return CsvWriter.toCsv(List.of("branch", "material_type", "checkouts", "returns", "renewals"),
                rows.stream().map(r -> List.of(r.branch(), r.materialType().name(), String.valueOf(r.checkouts()),
                        String.valueOf(r.returns()), String.valueOf(r.renewals()))).toList());
    }

    // ----- R-02 -----

    /** Open overdue loans with member, copy, due date and accrued fine. Scoped by the branch of checkout. */
    public List<OverdueRow> overdueItems(Long branchId) {
        LocalDate today = businessDate.today();
        return loans.findByStatusAndDueDateLessThanOrderByDueDate(LoanStatus.OPEN, today).stream()
                .filter(l -> branchId == null || l.getCheckoutBranch().getId().equals(branchId))
                .map(l -> toRow(l, today))
                .sorted(Comparator.comparing(OverdueRow::dueDate).thenComparing(OverdueRow::loanId))
                .toList();
    }

    public String overdueCsv(List<OverdueRow> rows) {
        return CsvWriter.toCsv(List.of("member", "card_number", "barcode", "title", "material_type", "due_date",
                        "days_overdue", "chargeable_days", "accrued_fine", "branch"),
                rows.stream().map(r -> List.of(r.memberName(), r.cardNumber(), r.barcode(), r.title(),
                        r.materialType().name(), r.dueDate().toString(), String.valueOf(r.daysOverdue()),
                        String.valueOf(r.chargeableDays()), r.accruedFine().toPlainString(), r.branch())).toList());
    }

    private OverdueRow toRow(Loan loan, LocalDate today) {
        FineResult accrued = fines.fineFor(loan, today);
        return new OverdueRow(loan.getId(), loan.getMember().fullName(), loan.getMember().getCardNumber(),
                loan.getCopy().getBarcode(), loan.getCopy().getTitle().getTitle(),
                loan.getCopy().getTitle().getMaterialType(), loan.getDueDate(),
                ChronoUnit.DAYS.between(loan.getDueDate(), today), accrued.chargeableDays(), accrued.fine(),
                loan.getCheckoutBranch().getCode());
    }
}
