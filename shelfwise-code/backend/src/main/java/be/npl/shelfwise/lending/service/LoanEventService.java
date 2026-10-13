package be.npl.shelfwise.lending.service;

import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.lending.entity.Channel;
import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.lending.entity.LoanEvent;
import be.npl.shelfwise.lending.entity.LoanEventType;
import be.npl.shelfwise.lending.repository.LoanEventRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records checkouts, renewals and returns for the circulation report R-01. */
@Service
@RequiredArgsConstructor
@Transactional
public class LoanEventService {

    private final LoanEventRepository events;

    public void record(Loan loan, LoanEventType type, LocalDate date, Branch branch, Channel channel) {
        LoanEvent event = new LoanEvent();
        event.setLoan(loan);
        event.setType(type);
        event.setEventDate(date);
        event.setBranch(branch);
        event.setChannel(channel);
        event.setMaterialType(loan.getCopy().getTitle().getMaterialType());
        events.save(event);
    }
}
