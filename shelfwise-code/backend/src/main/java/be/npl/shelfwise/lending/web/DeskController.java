package be.npl.shelfwise.lending.web;

import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.finance.service.AccountService;
import be.npl.shelfwise.finance.web.FinanceDtos.AccountView;
import be.npl.shelfwise.lending.service.LendingService;
import be.npl.shelfwise.lending.service.LoanQueryService;
import be.npl.shelfwise.lending.web.LendingDtos.ArrivalReceipt;
import be.npl.shelfwise.lending.web.LendingDtos.BarcodeRequest;
import be.npl.shelfwise.lending.web.LendingDtos.CheckoutReceipt;
import be.npl.shelfwise.lending.web.LendingDtos.CheckoutRequest;
import be.npl.shelfwise.lending.web.LendingDtos.LoanView;
import be.npl.shelfwise.lending.web.LendingDtos.ReturnReceipt;
import be.npl.shelfwise.reservation.service.ReservationService;
import be.npl.shelfwise.reservation.web.ReservationDtos.PlaceReservationRequest;
import be.npl.shelfwise.reservation.web.ReservationDtos.ReservationView;
import be.npl.shelfwise.security.CurrentUser;
import be.npl.shelfwise.security.Roles;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lending at the desk. Administrators never perform lending transactions (AC-ROL-02). */
@RestController
@RequestMapping("/api/desk")
@PreAuthorize(Roles.DESK)
@RequiredArgsConstructor
public class DeskController {

    private final LendingService lending;
    private final LoanQueryService loanQueries;
    private final ReservationService reservations;
    private final AccountService accounts;

    @PostMapping("/checkout")
    public CheckoutReceipt checkout(@Valid @RequestBody CheckoutRequest request) {
        return lending.checkout(request.cardNumber(), request.barcode(), CurrentUser.get());
    }

    @PostMapping("/returns")
    public ReturnReceipt returnCopy(@Valid @RequestBody BarcodeRequest request) {
        return lending.returnCopy(request.barcode(), CurrentUser.get());
    }

    @PostMapping("/arrivals")
    public ArrivalReceipt arrive(@Valid @RequestBody BarcodeRequest request) {
        return lending.arrive(request.barcode(), CurrentUser.get());
    }

    @PostMapping("/loans/{id}/renew")
    public LoanView renew(@PathVariable Long id) {
        return lending.renew(id, CurrentUser.get());
    }

    @GetMapping("/members/{memberId}/loans")
    public List<LoanView> openLoans(@PathVariable Long memberId) {
        return loanQueries.openLoansOf(memberId);
    }

    @GetMapping("/members/{memberId}/history")
    public List<LoanView> history(@PathVariable Long memberId) {
        return loanQueries.historyOf(memberId);
    }

    @GetMapping("/members/{memberId}/reservations")
    public List<ReservationView> reservations(@PathVariable Long memberId) {
        return reservations.listForMember(memberId);
    }

    @GetMapping("/members/{memberId}/account")
    public AccountView account(@PathVariable Long memberId) {
        return accounts.accountOf(memberId);
    }

    @PostMapping("/reservations")
    public ReservationView placeReservation(@Valid @RequestBody PlaceReservationRequest request) {
        if (request.memberId() == null) {
            throw new BusinessRuleException("MEMBER_REQUIRED", "F-RES-01", "Choose the member to reserve for.");
        }
        return reservations.place(request.memberId(), request.titleId(), request.pickupBranchId());
    }

    @PostMapping("/reservations/{id}/cancel")
    public ReservationView cancelReservation(@PathVariable Long id) {
        return reservations.cancel(id, CurrentUser.get());
    }
}
