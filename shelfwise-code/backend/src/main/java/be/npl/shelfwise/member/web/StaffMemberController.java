package be.npl.shelfwise.member.web;

import be.npl.shelfwise.member.service.MemberService;
import be.npl.shelfwise.member.web.MemberDtos.MemberDetail;
import be.npl.shelfwise.member.web.MemberDtos.MemberSummary;
import be.npl.shelfwise.member.web.MemberDtos.RegisterMemberRequest;
import be.npl.shelfwise.member.web.MemberDtos.RegistrationResult;
import be.npl.shelfwise.member.web.MemberDtos.RenewMembershipRequest;
import be.npl.shelfwise.member.web.MemberDtos.RenewalResult;
import be.npl.shelfwise.member.web.MemberDtos.StaffUpdateMemberRequest;
import be.npl.shelfwise.security.CurrentUser;
import be.npl.shelfwise.security.Roles;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Member administration at the desk. Librarians and branch managers only (section 3.2). */
@RestController
@RequestMapping("/api/staff/members")
@PreAuthorize(Roles.DESK)
@RequiredArgsConstructor
public class StaffMemberController {

    private final MemberService members;

    @GetMapping
    public List<MemberSummary> search(@RequestParam(defaultValue = "") String q) {
        return members.search(q);
    }

    @GetMapping("/duplicates")
    public List<MemberSummary> duplicates(@RequestParam String firstName, @RequestParam String lastName,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate) {
        return members.findDuplicates(firstName, lastName, birthDate);
    }

    @GetMapping("/{id}")
    public MemberDetail get(@PathVariable Long id) {
        return members.get(id);
    }

    @GetMapping("/by-card/{cardNumber}")
    public MemberDetail byCard(@PathVariable String cardNumber) {
        return members.getByCardNumber(cardNumber);
    }

    @PostMapping
    public RegistrationResult register(@Valid @RequestBody RegisterMemberRequest request) {
        return members.register(request, CurrentUser.get());
    }

    @PutMapping("/{id}")
    public MemberDetail update(@PathVariable Long id, @Valid @RequestBody StaffUpdateMemberRequest request) {
        return members.updateByStaff(id, request);
    }

    @PostMapping("/{id}/renew")
    public RenewalResult renew(@PathVariable Long id, @RequestBody RenewMembershipRequest request) {
        return members.renewAtDesk(id, request.paymentMethod(), CurrentUser.get());
    }
}
