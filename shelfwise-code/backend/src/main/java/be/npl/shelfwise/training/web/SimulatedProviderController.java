package be.npl.shelfwise.training.web;

import be.npl.shelfwise.payment.service.PaymentService;
import be.npl.shelfwise.payment.web.PaymentDtos.OnlinePaymentView;
import be.npl.shelfwise.payment.web.PaymentDtos.ProviderResultRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * F-TRN-05: the simulated payment provider. The page shows "Payment succeeds", "Payment fails" and "Cancel"; this is
 * the callback behind it. The unguessable payment reference identifies the payment.
 */
@RestController
@RequestMapping("/api/provider/payments")
@ConditionalOnProperty(name = "shelfwise.training-mode", havingValue = "true")
@RequiredArgsConstructor
public class SimulatedProviderController {

    private final PaymentService payments;

    @GetMapping("/{reference}")
    public OnlinePaymentView get(@PathVariable String reference) {
        return payments.sessionView(reference, null);
    }

    @PostMapping("/{reference}/result")
    public OnlinePaymentView result(@PathVariable String reference, @Valid @RequestBody ProviderResultRequest request) {
        return payments.completeOnlinePayment(reference, request.result());
    }
}
