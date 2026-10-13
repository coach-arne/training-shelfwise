package be.npl.shelfwise.security;

import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.common.NotFoundException;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.repository.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login for staff (username) and members (card number). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final StaffAccountRepository staffAccounts;
    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public record UserProfile(Role role, Long accountId, String displayName, Long branchId, String branchCode,
                              Long memberId, String username) {
    }

    public record LoginResult(String token, UserProfile profile) {
    }

    public LoginResult login(String username, String password) {
        Optional<StaffAccount> staff = staffAccounts.findByUsername(username).filter(StaffAccount::isActive);
        if (staff.isPresent()) {
            if (!passwordEncoder.matches(password, staff.get().getPasswordHash())) {
                throw invalidCredentials();
            }
            return loginAsStaff(staff.get());
        }
        Member member = members.findByCardNumber(username).orElseThrow(AuthService::invalidCredentials);
        if (!passwordEncoder.matches(password, member.getPasswordHash())) {
            throw invalidCredentials();
        }
        return loginAsMember(member);
    }

    /** Issues a token without a password. Only used by the demo login of training mode (F-TRN-06). */
    public LoginResult loginAsStaffId(Long staffId) {
        return loginAsStaff(staffAccounts.findById(staffId).orElseThrow(() -> new NotFoundException("Staff account", staffId)));
    }

    public LoginResult loginAsMemberId(Long memberId) {
        return loginAsMember(members.findById(memberId).orElseThrow(() -> new NotFoundException("Member", memberId)));
    }

    public UserProfile profile(CurrentUser user) {
        if (user.isMember()) {
            return memberProfile(members.findById(user.accountId())
                    .orElseThrow(() -> new NotFoundException("Member", user.accountId())));
        }
        return staffProfile(staffAccounts.findById(user.accountId())
                .orElseThrow(() -> new NotFoundException("Staff account", user.accountId())));
    }

    private LoginResult loginAsStaff(StaffAccount account) {
        UserProfile profile = staffProfile(account);
        String token = tokens.issue(account.getUsername(), account.getId(), account.getRole(), profile.branchId());
        return new LoginResult(token, profile);
    }

    private LoginResult loginAsMember(Member member) {
        String token = tokens.issue(member.getCardNumber(), member.getId(), Role.MEMBER, null);
        return new LoginResult(token, memberProfile(member));
    }

    private UserProfile staffProfile(StaffAccount account) {
        Long branchId = account.getBranch() == null ? null : account.getBranch().getId();
        String branchCode = account.getBranch() == null ? null : account.getBranch().getCode();
        return new UserProfile(account.getRole(), account.getId(), account.getDisplayName(), branchId, branchCode,
                account.getLinkedMember() == null ? null : account.getLinkedMember().getId(), account.getUsername());
    }

    private UserProfile memberProfile(Member member) {
        return new UserProfile(Role.MEMBER, member.getId(), member.fullName(), null, null, member.getId(),
                member.getCardNumber());
    }

    private static BusinessRuleException invalidCredentials() {
        return new BusinessRuleException("INVALID_CREDENTIALS", "F-SEC-01", "Invalid user name or password.");
    }
}
