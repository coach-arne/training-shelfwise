package be.npl.shelfwise.payment.web;

import be.npl.shelfwise.finance.service.WaiverService;
import be.npl.shelfwise.finance.web.FinanceDtos.ChargeView;
import be.npl.shelfwise.finance.web.FinanceDtos.WaiveRequest;
import be.npl.shelfwise.payment.service.PaymentService;
import be.npl.shelfwise.payment.web.PaymentDtos.DeskPaymentRequest;
import be.npl.shelfwise.payment.web.PaymentDtos.PaymentReceipt;
import be.npl.shelfwise.security.CurrentUser;
import be.npl.shelfwise.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Desk payments and waivers. Administrators cannot do either (section 3.2, AC-FIN-23). */
@RestController
@RequestMapping("/api/desk")
@PreAuthorize(Roles.DESK)
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService payments;
    private final WaiverService waivers;

    @PostMapping("/members/{memberId}/payments")
    public PaymentReceipt pay(@PathVariable Long memberId, @Valid @RequestBody DeskPaymentRequest request) {
        return payments.deskPayment(memberId, request.amount(), request.method(), CurrentUser.get());
    }

    @PostMapping("/charges/{chargeId}/waive")
    public ChargeView waive(@PathVariable Long chargeId, @Valid @RequestBody WaiveRequest request) {
        return waivers.waive(chargeId, request.amount(), request.reason(), CurrentUser.get());
    }
}
