package be.npl.shelfwise.member.web;

import be.npl.shelfwise.member.entity.MemberCategory;
import be.npl.shelfwise.member.service.MemberStatusService.BlockStatus;
import be.npl.shelfwise.payment.entity.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class MemberDtos {

    private MemberDtos() {
    }

    public record GuardianInfo(Long memberId, String name, String email) {
    }

    public record MemberDetail(
            Long id, String cardNumber, String firstName, String lastName, LocalDate birthDate, String street,
            String postalCode, String city, String email, String phone, boolean smsOptIn, boolean keepHistory,
            MemberCategory category, LocalDate membershipStart, LocalDate membershipEnd, GuardianInfo guardian,
            BlockStatus status, BigDecimal balance, long openLoans, int maxLoans, long activeReservations,
            long maxActiveReservations) {
    }

    public record MemberSummary(Long id, String cardNumber, String fullName, LocalDate birthDate,
                                MemberCategory category, String email, boolean blocked) {
    }

    public record RegisterMemberRequest(
            @NotBlank String firstName, @NotBlank String lastName, @NotNull @Past LocalDate birthDate,
            @NotBlank String street, @NotBlank String postalCode, @NotBlank String city,
            @NotBlank @Email String email, String phone, Long guardianMemberId, String guardianName,
            String guardianEmail, PaymentMethod paymentMethod, boolean confirmDuplicate) {
    }

    public record RegistrationResult(MemberDetail member, BigDecimal feePaid, String temporaryPassword) {
    }

    public record RenewMembershipRequest(PaymentMethod paymentMethod) {
    }

    public record RenewalResult(MemberDetail member, BigDecimal feePaid, MemberCategory previousCategory) {
    }

    public record StaffUpdateMemberRequest(
            @NotBlank String firstName, @NotBlank String lastName, @NotBlank String street,
            @NotBlank String postalCode, @NotBlank String city, @NotBlank @Email String email, String phone,
            boolean smsOptIn) {
    }

    public record PreferencesRequest(@NotBlank @Email String email, String phone, boolean smsOptIn,
                                     boolean keepHistory) {
    }
}
