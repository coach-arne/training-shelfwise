package be.npl.shelfwise.finance.service;

import be.npl.shelfwise.common.Money;
import be.npl.shelfwise.finance.entity.Charge;
import be.npl.shelfwise.finance.entity.ChargeStatus;
import be.npl.shelfwise.finance.entity.ChargeTransaction;
import be.npl.shelfwise.finance.entity.ChargeType;
import be.npl.shelfwise.finance.entity.TransactionType;
import be.npl.shelfwise.finance.repository.ChargeRepository;
import be.npl.shelfwise.finance.repository.ChargeTransactionRepository;
import be.npl.shelfwise.finance.web.FinanceDtos.ChargeView;
import be.npl.shelfwise.finance.web.FinanceDtos.TransactionView;
import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.reservation.entity.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The life cycle of charges (section 11.3): creation, payment, waiver and cancellation with refund. Every change
 * leaves a line in the charge history.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChargeService {

    private final ChargeRepository charges;
    private final ChargeTransactionRepository transactions;

    public Charge create(Member member, ChargeType type, BigDecimal amount, LocalDate date, Loan loan,
                         Reservation reservation, String description) {
        Charge charge = new Charge();
        charge.setMember(member);
        charge.setType(type);
        charge.setOriginalAmount(Money.round(amount));
        charge.setOutstandingAmount(Money.round(amount));
        charge.setChargeDate(date);
        charge.setStatus(ChargeStatus.OPEN);
        charge.setLoan(loan);
        charge.setReservation(reservation);
        charge.setDescription(description);
        return charges.save(charge);
    }

    /** Reduces the outstanding amount; the charge becomes SETTLED when nothing is outstanding. */
    public void applyPayment(Charge charge, BigDecimal amount, LocalDate date, String note) {
        reduceOutstanding(charge, amount);
        record(charge, TransactionType.PAYMENT, amount, date, note);
    }

    public void applyWaiver(Charge charge, BigDecimal amount, LocalDate date, String note) {
        reduceOutstanding(charge, amount);
        record(charge, TransactionType.WAIVER, amount, date, note);
    }

    /**
     * The charge no longer applies (BR-RES-03, BR-FIN-10): it becomes CANCELLED and any amount already paid on it is
     * refunded to the member.
     */
    public void cancel(Charge charge, LocalDate date, String reason) {
        if (charge.getStatus() == ChargeStatus.CANCELLED) {
            return;
        }
        BigDecimal paid = paidAmount(charge);
        BigDecimal cancelled = charge.getOutstandingAmount();
        charge.setOutstandingAmount(Money.ZERO);
        charge.setStatus(ChargeStatus.CANCELLED);
        charges.save(charge);
        record(charge, TransactionType.CANCELLATION, cancelled, date, reason);
        if (Money.isPositive(paid)) {
            record(charge, TransactionType.REFUND, paid, date, "Refund of the amount already paid");
        }
    }

    /** Payments minus refunds on a charge. */
    @Transactional(readOnly = true)
    public BigDecimal paidAmount(Charge charge) {
        BigDecimal paid = Money.ZERO;
        for (ChargeTransaction tx : transactions.findByChargeIdOrderByIdAsc(charge.getId())) {
            if (tx.getType() == TransactionType.PAYMENT) {
                paid = paid.add(tx.getAmount());
            } else if (tx.getType() == TransactionType.REFUND) {
                paid = paid.subtract(tx.getAmount());
            }
        }
        return paid;
    }

    @Transactional(readOnly = true)
    public List<ChargeView> viewsFor(Long memberId) {
        return charges.findByMemberIdOrderByChargeDateDescIdDesc(memberId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public ChargeView toView(Charge charge) {
        List<TransactionView> history = transactions.findByChargeIdOrderByIdAsc(charge.getId()).stream()
                .map(tx -> new TransactionView(tx.getType(), tx.getAmount(), tx.getTransactionDate(), tx.getNote()))
                .toList();
        return new ChargeView(charge.getId(), charge.getType(), charge.getOriginalAmount(),
                charge.getOutstandingAmount(), charge.getChargeDate(), charge.getStatus(), charge.getDescription(),
                charge.getLoan() == null ? null : charge.getLoan().getId(),
                charge.getReservation() == null ? null : charge.getReservation().getId(), history);
    }

    private void reduceOutstanding(Charge charge, BigDecimal amount) {
        BigDecimal outstanding = charge.getOutstandingAmount().subtract(amount);
        if (outstanding.signum() < 0) {
            throw new IllegalArgumentException("Amount exceeds the outstanding amount of charge " + charge.getId());
        }
        charge.setOutstandingAmount(Money.round(outstanding));
        if (outstanding.signum() == 0) {
            charge.setStatus(ChargeStatus.SETTLED);
        }
        charges.save(charge);
    }

    private void record(Charge charge, TransactionType type, BigDecimal amount, LocalDate date, String note) {
        ChargeTransaction tx = new ChargeTransaction();
        tx.setCharge(charge);
        tx.setType(type);
        tx.setAmount(Money.round(amount));
        tx.setTransactionDate(date);
        tx.setNote(note);
        transactions.save(tx);
    }
}
