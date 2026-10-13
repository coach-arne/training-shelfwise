package be.npl.shelfwise.portal;

import be.npl.shelfwise.finance.service.AccountService;
import be.npl.shelfwise.finance.web.FinanceDtos.AccountView;
import be.npl.shelfwise.lending.service.LendingService;
import be.npl.shelfwise.lending.service.LoanQueryService;
import be.npl.shelfwise.lending.web.LendingDtos.LoanView;
import be.npl.shelfwise.member.service.MemberService;
import be.npl.shelfwise.member.web.MemberDtos.MemberDetail;
import be.npl.shelfwise.member.web.MemberDtos.PreferencesRequest;
import be.npl.shelfwise.payment.service.PaymentService;
import be.npl.shelfwise.payment.web.PaymentDtos.OnlinePaymentView;
import be.npl.shelfwise.payment.web.PaymentDtos.StartOnlinePaymentRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Member portal (F-MEM-07). A member can only see and act on their own data: the member id always comes from the
 * authenticated user, never from the request (AC-ROL-03).
 */
@RestController
@RequestMapping("/api/portal")
@PreAuthorize(Roles.MEMBER)
@RequiredArgsConstructor
public class PortalController {

    private final MemberService members;
    private final LoanQueryService loanQueries;
    private final LendingService lending;
    private final AccountService accounts;
    private final ReservationService reservations;
    private final PaymentService payments;

    @GetMapping("/me")
    public MemberDetail me() {
        return members.get(memberId());
    }

    @PutMapping("/me/preferences")
    public MemberDetail updatePreferences(@Valid @RequestBody PreferencesRequest request) {
        return members.updatePreferences(memberId(), request);
    }

    @GetMapping("/loans")
    public List<LoanView> loans() {
        return loanQueries.openLoansOf(memberId());
    }

    @GetMapping("/loans/history")
    public List<LoanView> history() {
        return loanQueries.historyOf(memberId());
    }

    @PostMapping("/loans/{id}/renew")
    public LoanView renew(@PathVariable Long id) {
        return lending.renew(id, CurrentUser.get());
    }

    @GetMapping("/account")
    public AccountView account() {
        return accounts.accountOf(memberId());
    }

    @GetMapping("/reservations")
    public List<ReservationView> reservations() {
        return reservations.listForMember(memberId());
    }

    @PostMapping("/reservations")
    public ReservationView reserve(@Valid @RequestBody PlaceReservationRequest request) {
        return reservations.place(memberId(), request.titleId(), request.pickupBranchId());
    }

    @PostMapping("/reservations/{id}/cancel")
    public ReservationView cancelReservation(@PathVariable Long id) {
        return reservations.cancel(id, CurrentUser.get());
    }

    @PostMapping("/payments")
    public OnlinePaymentView startPayment(@RequestBody StartOnlinePaymentRequest request) {
        return payments.startOnlinePayment(memberId(), request.chargeIds());
    }

    @GetMapping("/payments/{reference}")
    public OnlinePaymentView payment(@PathVariable String reference) {
        return payments.sessionView(reference, memberId());
    }

    private static Long memberId() {
        return CurrentUser.get().accountId();
    }
}
