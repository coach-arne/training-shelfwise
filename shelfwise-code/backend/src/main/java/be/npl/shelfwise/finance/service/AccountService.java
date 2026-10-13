package be.npl.shelfwise.finance.service;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.common.NotFoundException;
import be.npl.shelfwise.finance.web.FinanceDtos.AccountView;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.repository.MemberRepository;
import be.npl.shelfwise.member.service.MemberStatusService;
import be.npl.shelfwise.member.service.MemberStatusService.BlockStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The account of a member: balance, blocking and every charge with its history (F-PAY-03). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final MemberRepository members;
    private final MemberStatusService memberStatus;
    private final ChargeService charges;
    private final BusinessDateService businessDate;

    public AccountView accountOf(Long memberId) {
        Member member = members.findById(memberId).orElseThrow(() -> new NotFoundException("Member", memberId));
        BlockStatus status = memberStatus.blockStatus(member, businessDate.today());
        return new AccountView(memberId, status.balance(), status.blocked(), status.reasons(),
                charges.viewsFor(memberId));
    }
}
