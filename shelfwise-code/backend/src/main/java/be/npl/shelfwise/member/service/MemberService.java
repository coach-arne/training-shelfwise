package be.npl.shelfwise.member.service;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.catalogue.repository.BranchRepository;
import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.common.NotFoundException;
import be.npl.shelfwise.config.RulesProperties;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.entity.MemberCategory;
import be.npl.shelfwise.member.entity.MembershipFeePayment;
import be.npl.shelfwise.member.helper.CardNumbers;
import be.npl.shelfwise.member.helper.CategoryResolver;
import be.npl.shelfwise.member.helper.MembershipPeriod;
import be.npl.shelfwise.member.helper.MembershipPeriod.Period;
import be.npl.shelfwise.member.repository.MemberRepository;
import be.npl.shelfwise.member.repository.MembershipFeePaymentRepository;
import be.npl.shelfwise.member.web.MemberDtos.GuardianInfo;
import be.npl.shelfwise.member.web.MemberDtos.MemberDetail;
import be.npl.shelfwise.member.web.MemberDtos.MemberSummary;
import be.npl.shelfwise.member.web.MemberDtos.PreferencesRequest;
import be.npl.shelfwise.member.web.MemberDtos.RegisterMemberRequest;
import be.npl.shelfwise.member.web.MemberDtos.RegistrationResult;
import be.npl.shelfwise.member.web.MemberDtos.RenewalResult;
import be.npl.shelfwise.member.web.MemberDtos.StaffUpdateMemberRequest;
import be.npl.shelfwise.payment.entity.PaymentMethod;
import be.npl.shelfwise.reservation.entity.ReservationStatus;
import be.npl.shelfwise.reservation.repository.ReservationRepository;
import be.npl.shelfwise.security.CurrentUser;
import be.npl.shelfwise.security.StaffAccountRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration, membership renewal, contact data and member lookups (F-MEM-01 to F-MEM-07). */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private static final String PASSWORD_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";

    private final MemberRepository members;
    private final MembershipFeePaymentRepository feePayments;
    private final StaffAccountRepository staffAccounts;
    private final BranchRepository branches;
    private final ReservationRepository reservations;
    private final MemberStatusService status;
    private final BusinessDateService businessDate;
    private final PasswordEncoder passwordEncoder;
    private final RulesProperties rules;
    private final SecureRandom random = new SecureRandom();

    // ----- lookups -----

    @Transactional(readOnly = true)
    public MemberDetail get(Long id) {
        return toDetail(find(id));
    }

    @Transactional(readOnly = true)
    public MemberDetail getByCardNumber(String cardNumber) {
        return toDetail(members.findByCardNumber(cardNumber.trim())
                .orElseThrow(() -> new NotFoundException("Member with card", cardNumber)));
    }

    @Transactional(readOnly = true)
    public List<MemberSummary> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        LocalDate today = businessDate.today();
        return members.search("%" + query.trim().toLowerCase() + "%", PageRequest.of(0, 50)).stream()
                .map(m -> new MemberSummary(m.getId(), m.getCardNumber(), m.fullName(), m.getBirthDate(),
                        m.getCategory(), m.getEmail(), status.blockStatus(m, today).blocked()))
                .toList();
    }

    /** F-MEM-02 step 1: people with the same name and date of birth. */
    @Transactional(readOnly = true)
    public List<MemberSummary> findDuplicates(String firstName, String lastName, LocalDate birthDate) {
        LocalDate today = businessDate.today();
        return members.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDate(firstName.trim(), lastName.trim(), birthDate)
                .stream()
                .map(m -> new MemberSummary(m.getId(), m.getCardNumber(), m.fullName(), m.getBirthDate(),
                        m.getCategory(), m.getEmail(), status.blockStatus(m, today).blocked()))
                .toList();
    }

    // ----- registration and renewal -----

    /** F-MEM-02: register a member at the desk. */
    public RegistrationResult register(RegisterMemberRequest request, CurrentUser staff) {
        LocalDate today = businessDate.today();
        if (!request.confirmDuplicate()
                && !findDuplicates(request.firstName(), request.lastName(), request.birthDate()).isEmpty()) {
            throw new BusinessRuleException("POSSIBLE_DUPLICATE", "F-MEM-02",
                    "A member with the same name and date of birth is already registered.");
        }
        MemberCategory category = CategoryResolver.resolve(request.birthDate(), today);
        BigDecimal fee = category.membershipFee();
        PaymentMethod method = feeMethod(fee, request.paymentMethod());

        Member member = new Member();
        member.setFirstName(request.firstName().trim());
        member.setLastName(request.lastName().trim());
        member.setBirthDate(request.birthDate());
        member.setStreet(request.street().trim());
        member.setPostalCode(request.postalCode().trim());
        member.setCity(request.city().trim());
        member.setEmail(request.email().trim());
        member.setPhone(blankToNull(request.phone()));
        member.setCategory(category);
        member.setRegistrationDate(today);
        Period period = MembershipPeriod.startingOn(today, rules.membershipMonths());
        member.setMembershipStart(period.start());
        member.setMembershipEnd(period.end());
        applyGuardian(member, request, category);

        String temporaryPassword = generatePassword();
        member.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        member.setCardNumber(CardNumbers.format(members.nextCardSequence()));
        members.save(member);
        recordFee(member, fee, method, today, "REGISTRATION", staff);
        return new RegistrationResult(toDetail(member), fee, temporaryPassword);
    }

    /** F-MEM-04: renew the membership at the desk. The category is re-evaluated (BR-MEM-01). */
    public RenewalResult renewAtDesk(Long memberId, PaymentMethod requestedMethod, CurrentUser staff) {
        Member member = find(memberId);
        LocalDate today = businessDate.today();
        MemberCategory previous = member.getCategory();
        MemberCategory category = CategoryResolver.resolve(member.getBirthDate(), today);
        BigDecimal fee = category.membershipFee();
        PaymentMethod method = feeMethod(fee, requestedMethod);
        applyRenewal(member, category, today);
        recordFee(member, fee, method, today, "RENEWAL", staff);
        return new RenewalResult(toDetail(member), fee, previous);
    }

    private void applyRenewal(Member member, MemberCategory category, LocalDate today) {
        Period period = MembershipPeriod.renewal(member.getMembershipEnd(), today, rules.membershipMonths());
        member.setCategory(category);
        member.setMembershipStart(period.start());
        member.setMembershipEnd(period.end());
        members.save(member);
    }

    // ----- updates -----

    public MemberDetail updateByStaff(Long memberId, StaffUpdateMemberRequest request) {
        Member member = find(memberId);
        member.setFirstName(request.firstName().trim());
        member.setLastName(request.lastName().trim());
        member.setStreet(request.street().trim());
        member.setPostalCode(request.postalCode().trim());
        member.setCity(request.city().trim());
        member.setEmail(request.email().trim());
        member.setPhone(blankToNull(request.phone()));
        member.setSmsOptIn(request.smsOptIn());
        return toDetail(members.save(member));
    }

    /** F-MEM-07: e-mail address, phone number, SMS opt-in and the "Keep my reading history" opt-in. */
    public MemberDetail updatePreferences(Long memberId, PreferencesRequest request) {
        Member member = find(memberId);
        member.setEmail(request.email().trim());
        member.setPhone(blankToNull(request.phone()));
        member.setSmsOptIn(request.smsOptIn());
        member.setKeepHistory(request.keepHistory());
        return toDetail(members.save(member));
    }

    // ----- mapping -----

    public MemberDetail toDetail(Member member) {
        LocalDate today = businessDate.today();
        GuardianInfo guardian = null;
        if (member.getGuardianMember() != null) {
            guardian = new GuardianInfo(member.getGuardianMember().getId(), member.getGuardianMember().fullName(),
                    member.getGuardianMember().getEmail());
        } else if (member.getGuardianName() != null) {
            guardian = new GuardianInfo(null, member.getGuardianName(), member.getGuardianEmail());
        }
        var blockStatus = status.blockStatus(member, today);
        long activeReservations = reservations.countByMemberIdAndStatusIn(member.getId(),
                Set.of(ReservationStatus.WAITING, ReservationStatus.ON_HOLD));
        return new MemberDetail(member.getId(), member.getCardNumber(), member.getFirstName(), member.getLastName(),
                member.getBirthDate(), member.getStreet(), member.getPostalCode(), member.getCity(), member.getEmail(),
                member.getPhone(), member.isSmsOptIn(), member.isKeepHistory(), member.getCategory(),
                member.getMembershipStart(), member.getMembershipEnd(), guardian, blockStatus, blockStatus.balance(),
                status.openLoanCount(member.getId()), status.maxLoans(member.getCategory()), activeReservations,
                rules.maxActiveReservations());
    }

    // ----- helpers -----

    private Member find(Long id) {
        return members.findById(id).orElseThrow(() -> new NotFoundException("Member", id));
    }

    /** F-MEM-02 step 3: a YOUTH member needs a guardian (ADULT/SENIOR member, or name and e-mail only). */
    private void applyGuardian(Member member, RegisterMemberRequest request, MemberCategory category) {
        if (category != MemberCategory.YOUTH) {
            return;
        }
        if (request.guardianMemberId() != null) {
            Member guardian = find(request.guardianMemberId());
            if (guardian.getCategory() == MemberCategory.YOUTH) {
                throw new BusinessRuleException("INVALID_GUARDIAN", "F-MEM-02",
                        "A guardian must be an ADULT or SENIOR member.");
            }
            member.setGuardianMember(guardian);
        } else if (notBlank(request.guardianName()) && notBlank(request.guardianEmail())) {
            member.setGuardianName(request.guardianName().trim());
            member.setGuardianEmail(request.guardianEmail().trim());
        } else {
            throw new BusinessRuleException("GUARDIAN_REQUIRED", "F-MEM-02",
                    "A YOUTH member must be linked to a guardian.");
        }
    }

    private PaymentMethod feeMethod(BigDecimal fee, PaymentMethod requested) {
        if (fee.signum() == 0) {
            return null;
        }
        if (requested != PaymentMethod.CASH && requested != PaymentMethod.DEBIT_CARD) {
            throw new BusinessRuleException("PAYMENT_METHOD_REQUIRED", "BR-MEM-04",
                    "The membership fee of EUR " + fee + " is paid at the desk in cash or by debit card.");
        }
        return requested;
    }

    private void recordFee(Member member, BigDecimal fee, PaymentMethod method, LocalDate today, String kind,
                           CurrentUser staff) {
        MembershipFeePayment payment = new MembershipFeePayment();
        payment.setMember(member);
        payment.setAmount(fee);
        payment.setMethod(method);
        payment.setPaidOn(today);
        payment.setKind(kind);
        if (staff != null) {
            payment.setStaff(staffAccounts.getReferenceById(staff.accountId()));
            if (staff.branchId() != null) {
                payment.setBranch(branches.getReferenceById(staff.branchId()));
            }
        }
        feePayments.save(payment);
    }

    private String generatePassword() {
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        return notBlank(value) ? value.trim() : null;
    }
}
