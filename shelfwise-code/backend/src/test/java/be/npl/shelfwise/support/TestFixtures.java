package be.npl.shelfwise.support;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.catalogue.entity.Title;
import be.npl.shelfwise.catalogue.repository.BranchRepository;
import be.npl.shelfwise.catalogue.repository.CopyRepository;
import be.npl.shelfwise.catalogue.repository.TitleRepository;
import be.npl.shelfwise.finance.entity.Charge;
import be.npl.shelfwise.finance.entity.ChargeType;
import be.npl.shelfwise.finance.repository.ChargeRepository;
import be.npl.shelfwise.finance.service.ChargeService;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.repository.MemberRepository;
import be.npl.shelfwise.member.service.MemberStatusService;
import be.npl.shelfwise.notification.entity.Notification;
import be.npl.shelfwise.notification.repository.NotificationRepository;
import be.npl.shelfwise.processing.DailyProcessingService;
import be.npl.shelfwise.security.CurrentUser;
import be.npl.shelfwise.security.Role;
import be.npl.shelfwise.security.StaffAccount;
import be.npl.shelfwise.security.StaffAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Building blocks shared by the integration tests: seed lookups, fresh copies, dates and outbox queries. */
@Component
@RequiredArgsConstructor
public class TestFixtures {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final MemberRepository members;
    private final StaffAccountRepository staffAccounts;
    private final BranchRepository branches;
    private final TitleRepository titles;
    private final CopyRepository copies;
    private final ChargeRepository charges;
    private final ChargeService chargeService;
    private final NotificationRepository notifications;
    private final BusinessDateService businessDate;
    private final DailyProcessingService processing;
    private final MemberStatusService memberStatus;

    /** Member S-nn of the seed. */
    public Member member(int number) {
        return members.findByCardNumber(String.format("NPL%06d", number)).orElseThrow();
    }

    public Member reload(Member member) {
        return members.findById(member.getId()).orElseThrow();
    }

    public CurrentUser staff(String username) {
        StaffAccount account = staffAccounts.findByUsername(username).orElseThrow();
        return new CurrentUser(account.getId(), account.getRole(), account.getBranch() == null ? null : account.getBranch().getId());
    }

    public CurrentUser librarianCen() {
        return staff("librarian.cen");
    }

    public CurrentUser librarianRiv() {
        return staff("librarian.riv");
    }

    public CurrentUser managerCen() {
        return staff("manager.cen");
    }

    public CurrentUser admin() {
        return staff("admin");
    }

    public CurrentUser asMember(Member member) {
        return new CurrentUser(member.getId(), Role.MEMBER, null);
    }

    /** Makes a staff account the same person as a member (section 3.2, own-account rule). */
    public void linkStaffToMember(String username, Member member) {
        StaffAccount account = staffAccounts.findByUsername(username).orElseThrow();
        account.setLinkedMember(member);
        staffAccounts.save(account);
    }

    public Branch branch(String code) {
        return branches.findByCode(code).orElseThrow();
    }

    /** A brand new title with one available copy at the given branch. */
    public Copy newCopy(MaterialType type, String branchCode) {
        Title title = newTitle(type);
        return newCopyOf(title, branchCode);
    }

    public Title newTitle(MaterialType type) {
        Title title = new Title();
        title.setTitle("Test title " + SEQUENCE.incrementAndGet());
        title.setAuthors("Test Author");
        title.setMaterialType(type);
        return titles.save(title);
    }

    public Copy newCopyOf(Title title, String branchCode) {
        Branch branch = branch(branchCode);
        Copy copy = new Copy();
        copy.setBarcode("T" + System.nanoTime() + SEQUENCE.incrementAndGet());
        copy.setTitle(title);
        copy.setHomeBranch(branch);
        copy.setCurrentBranch(branch);
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setAcquisitionDate(LocalDate.of(2025, 1, 1));
        return copies.save(copy);
    }

    public Copy reload(Copy copy) {
        return copies.findById(copy.getId()).orElseThrow();
    }

    /** Sets the business date; every date before it counts as processed. */
    public void today(LocalDate date) {
        businessDate.setBusinessDate(date);
        businessDate.setLastProcessedDate(date.minusDays(1));
    }

    /** Runs the daily processing for every date up to and including {@code through}. */
    public void processThrough(LocalDate through) {
        processing.catchUp(through);
    }

    public Charge addCharge(Member member, ChargeType type, String amount, LocalDate date) {
        return chargeService.create(member, type, new BigDecimal(amount), date, null, null, "test charge");
    }

    public BigDecimal balance(Member member) {
        return memberStatus.balance(member.getId());
    }

    public List<Charge> chargesOf(Member member) {
        return charges.findByMemberIdOrderByChargeDateDescIdDesc(member.getId());
    }

    public Charge charge(Long id) {
        return charges.findById(id).orElseThrow();
    }

    public List<Notification> notificationsOf(Member member) {
        return notifications.findAll().stream()
                .filter(n -> n.getMember() != null && n.getMember().getId().equals(member.getId()))
                .toList();
    }

    public List<Notification> notificationsOf(Member member, String typeCode) {
        return notificationsOf(member).stream().filter(n -> List.of(n.getType().split(",")).contains(typeCode)).toList();
    }
}
