package be.npl.shelfwise.notification.helper;

import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.entity.MemberCategory;

/** F-NOT-01: notifications for YOUTH members go to the guardian; SMS is never used for YOUTH members. */
public final class RecipientResolver {

    private RecipientResolver() {
    }

    public static String emailAddress(Member member) {
        if (member.getCategory() == MemberCategory.YOUTH) {
            if (member.getGuardianMember() != null) {
                return member.getGuardianMember().getEmail();
            }
            if (member.getGuardianEmail() != null && !member.getGuardianEmail().isBlank()) {
                return member.getGuardianEmail();
            }
        }
        return member.getEmail();
    }

    public static String greetingName(Member member) {
        if (member.getCategory() == MemberCategory.YOUTH) {
            String guardian = member.getGuardianMember() != null ? member.getGuardianMember().fullName()
                    : member.getGuardianName();
            return (guardian == null ? "guardian" : guardian) + " (guardian of " + member.fullName() + ")";
        }
        return member.fullName();
    }

    public static boolean receivesSms(Member member) {
        return member.isSmsOptIn() && member.getCategory() != MemberCategory.YOUTH
                && member.getPhone() != null && !member.getPhone().isBlank();
    }
}
