package be.npl.shelfwise.training.web;

import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.helper.CardNumbers;
import be.npl.shelfwise.member.repository.MemberRepository;
import be.npl.shelfwise.security.AuthService;
import be.npl.shelfwise.security.Role;
import be.npl.shelfwise.security.StaffAccountRepository;
import be.npl.shelfwise.seed.SeedScenarios;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** F-TRN-06: log in as one of the seed accounts (Appendix B.1) without a password. Training mode only. */
@RestController
@RequestMapping("/api/demo")
@ConditionalOnProperty(name = "shelfwise.training-mode", havingValue = "true")
@RequiredArgsConstructor
public class DemoLoginController {

    private final StaffAccountRepository staffAccounts;
    private final MemberRepository members;
    private final AuthService auth;

    public record DemoAccount(String type, Long id, String username, String displayName, Role role,
                              String branchCode, String scenario, String description) {
    }

    public record DemoLoginRequest(@NotNull String type, @NotNull Long id) {
    }

    @GetMapping("/accounts")
    @Transactional(readOnly = true)
    public List<DemoAccount> accounts() {
        List<DemoAccount> result = new ArrayList<>();
        staffAccounts.findAllByActiveTrueOrderByUsername().forEach(a -> result.add(new DemoAccount(
                "STAFF", a.getId(), a.getUsername(), a.getDisplayName(), a.getRole(),
                a.getBranch() == null ? null : a.getBranch().getCode(), null,
                a.getLinkedMember() != null ? "Same person as member " + a.getLinkedMember().fullName() : null)));
        for (int i = 0; i < SeedScenarios.MEMBERS.size(); i++) {
            SeedScenarios.Scenario scenario = SeedScenarios.MEMBERS.get(i);
            members.findByCardNumber(CardNumbers.format(i + 1L))
                    .filter(m -> m.getLastName().equals(scenario.lastName()) && m.getFirstName().equals(scenario.firstName()))
                    .ifPresent(m -> result.add(memberAccount(m, scenario)));
        }
        return result;
    }

    @PostMapping("/login")
    public AuthService.LoginResult login(@Valid @RequestBody DemoLoginRequest request) {
        return "STAFF".equals(request.type()) ? auth.loginAsStaffId(request.id()) : auth.loginAsMemberId(request.id());
    }

    private static DemoAccount memberAccount(Member m, SeedScenarios.Scenario scenario) {
        return new DemoAccount("MEMBER", m.getId(), m.getCardNumber(), m.fullName(), Role.MEMBER, null,
                scenario.code(), scenario.purpose());
    }
}
