package be.npl.shelfwise.payment.service;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.catalogue.repository.BranchRepository;
import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.common.Money;
import be.npl.shelfwise.common.NotFoundException;
import be.npl.shelfwise.finance.entity.Charge;
import be.npl.shelfwise.finance.entity.ChargeStatus;
import be.npl.shelfwise.finance.helper.PaymentAllocator;
import be.npl.shelfwise.finance.helper.PaymentAllocator.Allocation;
import be.npl.shelfwise.finance.helper.PaymentAllocator.ChargeOutstanding;
import be.npl.shelfwise.finance.repository.ChargeRepository;
import be.npl.shelfwise.finance.service.ChargeService;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.repository.MemberRepository;
import be.npl.shelfwise.member.service.MemberStatusService;
import be.npl.shelfwise.notification.entity.NotificationType;
import be.npl.shelfwise.notification.service.NotificationService;
import be.npl.shelfwise.payment.entity.OnlinePaymentSession;
import be.npl.shelfwise.payment.entity.Payment;
import be.npl.shelfwise.payment.entity.PaymentAllocation;
import be.npl.shelfwise.payment.entity.PaymentMethod;
import be.npl.shelfwise.payment.entity.PaymentSessionStatus;
import be.npl.shelfwise.payment.repository.OnlinePaymentSessionRepository;
import be.npl.shelfwise.payment.repository.PaymentAllocationRepository;
import be.npl.shelfwise.payment.repository.PaymentRepository;
import be.npl.shelfwise.payment.web.PaymentDtos.AllocationView;
import be.npl.shelfwise.payment.web.PaymentDtos.OnlinePaymentView;
import be.npl.shelfwise.payment.web.PaymentDtos.PaymentReceipt;
import be.npl.shelfwise.payment.web.PaymentDtos.ProviderResult;
import be.npl.shelfwise.security.CurrentUser;
import be.npl.shelfwise.security.StaffAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Desk payments (F-PAY-01) and online payments through the simulated provider (F-PAY-02, F-TRN-05). */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final MemberRepository members;
    private final ChargeRepository charges;
    private final ChargeService chargeService;
    private final PaymentRepository payments;
    private final PaymentAllocationRepository allocations;
    private final OnlinePaymentSessionRepository sessions;
    private final StaffAccountRepository staffAccounts;
    private final BranchRepository branches;
    private final MemberStatusService memberStatus;
    private final NotificationService notifications;
    private final BusinessDateService businessDate;

    // ----- desk payment -----

    /**
     * Partial payments are allowed. A desk payment cannot be higher than the account balance (BR-FIN-15) and is
     * allocated to the oldest outstanding charge first (BR-FIN-09). Allowed for blocked members and expired
     * memberships.
     */
    public PaymentReceipt deskPayment(Long memberId, BigDecimal amount, PaymentMethod method, CurrentUser staff) {
        if (method != PaymentMethod.CASH && method != PaymentMethod.DEBIT_CARD) {
            throw new BusinessRuleException("INVALID_PAYMENT_METHOD", "F-PAY-01",
                    "A desk payment is made in cash or by debit card.");
        }
        Member member = members.findById(memberId).orElseThrow(() -> new NotFoundException("Member", memberId));
        BigDecimal paid = Money.round(amount);
        if (!Money.isPositive(paid)) {
            throw new BusinessRuleException("INVALID_AMOUNT", "F-PAY-01", "The amount must be greater than zero.");
        }
        BigDecimal balance = memberStatus.balance(memberId);
        if (Money.gt(paid, balance)) {
            throw new BusinessRuleException("PAYMENT_EXCEEDS_BALANCE", "BR-FIN-15",
                    "The payment of EUR " + paid + " exceeds the account balance of EUR " + balance + ".");
        }
        List<Charge> open = charges.findByMemberIdAndStatusOrderByChargeDateAscIdAsc(memberId, ChargeStatus.OPEN);
        List<Allocation> toApply = PaymentAllocator.allocateOldestFirst(
                open.stream().map(c -> new ChargeOutstanding(c.getId(), c.getOutstandingAmount())).toList(), paid);

        Payment payment = newPayment(member, paid, method, null);
        payment.setStaff(staffAccounts.getReferenceById(staff.accountId()));
        if (staff.branchId() != null) {
            payment.setBranch(branches.getReferenceById(staff.branchId()));
        }
        payments.save(payment);
        return apply(payment, member, open, toApply);
    }

    // ----- online payment -----

    /** The member chooses to pay the full balance (no charges given) or a selection of charges, each in full. */
    public OnlinePaymentView startOnlinePayment(Long memberId, List<Long> chargeIds) {
        Member member = members.findById(memberId).orElseThrow(() -> new NotFoundException("Member", memberId));
        List<Charge> open = charges.findByMemberIdAndStatusOrderByChargeDateAscIdAsc(memberId, ChargeStatus.OPEN);
        List<Charge> selected;
        if (chargeIds == null || chargeIds.isEmpty()) {
            selected = open;
        } else {
            Map<Long, Charge> byId = open.stream().collect(Collectors.toMap(Charge::getId, Function.identity()));
            selected = new ArrayList<>();
            for (Long id : new HashSet<>(chargeIds)) {
                Charge charge = byId.get(id);
                if (charge == null) {
                    throw new BusinessRuleException("CHARGE_NOT_PAYABLE", "F-PAY-02",
                            "Charge " + id + " is not an open charge of this member.");
                }
                selected.add(charge);
            }
        }
        BigDecimal total = selected.stream().map(Charge::getOutstandingAmount).reduce(Money.ZERO, BigDecimal::add);
        if (!Money.isPositive(total)) {
            throw new BusinessRuleException("NOTHING_TO_PAY", "F-PAY-02", "There is nothing to pay.");
        }
        OnlinePaymentSession session = new OnlinePaymentSession();
        session.setReference(UUID.randomUUID().toString());
        session.setMember(member);
        session.setAmount(total);
        session.setStatus(PaymentSessionStatus.PENDING);
        session.setCreatedOn(businessDate.today());
        session.setChargeIds(selected.stream().map(Charge::getId).collect(Collectors.toSet()));
        sessions.save(session);
        return view(session, "Waiting for the payment provider.");
    }

    @Transactional(readOnly = true)
    public OnlinePaymentView sessionView(String reference, Long memberIdOrNull) {
        OnlinePaymentSession session = sessions.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Payment", reference));
        if (memberIdOrNull != null && !session.getMember().getId().equals(memberIdOrNull)) {
            throw new org.springframework.security.access.AccessDeniedException("Not your payment");
        }
        return view(session, messageFor(session.getStatus()));
    }

    /**
     * The payment provider reports a result. The same result is never processed twice (AC-PAY-05): the session row
     * is locked and only a PENDING session is processed; the payment also has a unique provider reference.
     */
    public OnlinePaymentView completeOnlinePayment(String reference, ProviderResult result) {
        OnlinePaymentSession session = sessions.findByReferenceForUpdate(reference)
                .orElseThrow(() -> new NotFoundException("Payment", reference));
        if (session.getStatus() != PaymentSessionStatus.PENDING) {
            return view(session, messageFor(session.getStatus()));
        }
        if (result != ProviderResult.SUCCEEDS) {
            session.setStatus(result == ProviderResult.FAILS ? PaymentSessionStatus.FAILED : PaymentSessionStatus.CANCELLED);
            sessions.save(session);
            return view(session, messageFor(session.getStatus()));
        }
        Member member = session.getMember();
        List<Charge> selected = charges.findAllById(session.getChargeIds()).stream()
                .filter(c -> c.getStatus() == ChargeStatus.OPEN)
                .sorted(java.util.Comparator.comparing(Charge::getChargeDate).thenComparing(Charge::getId))
                .toList();
        // Each selected charge is paid in full (F-PAY-02).
        List<Allocation> toApply = selected.stream().map(c -> new Allocation(c.getId(), c.getOutstandingAmount())).toList();
        BigDecimal total = toApply.stream().map(Allocation::amount).reduce(Money.ZERO, BigDecimal::add);
        if (Money.isPositive(total)) {
            Payment payment = newPayment(member, total, PaymentMethod.ONLINE, reference);
            payments.save(payment);
            apply(payment, member, selected, toApply);
        }
        session.setStatus(PaymentSessionStatus.SUCCEEDED);
        sessions.save(session);
        return view(session, messageFor(PaymentSessionStatus.SUCCEEDED));
    }

    // ----- helpers -----

    private Payment newPayment(Member member, BigDecimal amount, PaymentMethod method, String providerReference) {
        Payment payment = new Payment();
        payment.setMember(member);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setPaymentDate(businessDate.today());
        payment.setProviderReference(providerReference);
        return payment;
    }

    private PaymentReceipt apply(Payment payment, Member member, List<Charge> candidates, List<Allocation> toApply) {
        LocalDate today = payment.getPaymentDate();
        Map<Long, Charge> byId = candidates.stream().collect(Collectors.toMap(Charge::getId, Function.identity()));
        List<AllocationView> views = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        for (Allocation allocation : toApply) {
            Charge charge = byId.get(allocation.chargeId());
            chargeService.applyPayment(charge, allocation.amount(), today, payment.getMethod() + " payment");
            PaymentAllocation row = new PaymentAllocation();
            row.setPayment(payment);
            row.setCharge(charge);
            row.setAmount(allocation.amount());
            allocations.save(row);
            views.add(new AllocationView(charge.getId(), charge.getType(), allocation.amount(), charge.getOutstandingAmount()));
            lines.add("EUR " + allocation.amount() + " paid on " + charge.getType() + " of " + charge.getChargeDate()
                    + " (outstanding: EUR " + charge.getOutstandingAmount() + ")");
        }
        var status = memberStatus.blockStatus(member, today);
        lines.add(0, "We received your " + payment.getMethod() + " payment of EUR " + payment.getAmount() + ".");
        lines.add("New account balance: EUR " + status.balance());
        notifications.sendNow(NotificationType.N_10, member, today, lines);
        return new PaymentReceipt(payment.getId(), member.getId(), member.fullName(), payment.getAmount(),
                payment.getMethod(), today, views, status.balance(), status.blocked());
    }

    private static OnlinePaymentView view(OnlinePaymentSession session, String message) {
        return new OnlinePaymentView(session.getReference(), session.getAmount(), session.getStatus(),
                new ArrayList<>(new java.util.TreeSet<>(session.getChargeIds())), message);
    }

    private static String messageFor(PaymentSessionStatus status) {
        return switch (status) {
            case PENDING -> "Waiting for the payment provider.";
            case SUCCEEDED -> "The payment succeeded. Thank you.";
            case FAILED -> "The payment failed. Nothing was charged and your account is unchanged.";
            case CANCELLED -> "You cancelled the payment. Nothing was charged and your account is unchanged.";
        };
    }
}
