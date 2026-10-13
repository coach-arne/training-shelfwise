package be.npl.shelfwise;

import static org.assertj.core.api.Assertions.assertThat;

import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.lending.service.LendingService;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.service.MemberService;
import be.npl.shelfwise.member.web.MemberDtos.PreferencesRequest;
import be.npl.shelfwise.notification.entity.Notification;
import be.npl.shelfwise.notification.entity.NotificationChannel;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Appendix A.8: notifications and daily processing. */
class NotificationIT extends IntegrationTestBase {

    @Autowired LendingService lending;
    @Autowired MemberService memberService;

    private Copy checkoutOnMarch2(Member member) {
        Copy copy = fx.newCopy(MaterialType.BOOK, "CEN");
        lending.checkout(member.getCardNumber(), copy.getBarcode(), fx.librarianCen());
        return copy;
    }

    private static List<LocalDate> dates(List<Notification> notifications) {
        return notifications.stream().map(Notification::getNotificationDate).distinct().sorted().toList();
    }

    private List<Notification> emails(Member member, String type) {
        return fx.notificationsOf(member, type).stream().filter(n -> n.getChannel() == NotificationChannel.EMAIL).toList();
    }

    @Test
    @DisplayName("AC-NOT-01 a loan that is never returned triggers N-01 to N-05 on the specified days")
    void neverReturned() {
        fx.today(LocalDate.of(2026, 3, 2));
        Member ada = fx.member(1);
        Copy copy = checkoutOnMarch2(ada);
        fx.processThrough(LocalDate.of(2026, 5, 4));

        assertThat(dates(emails(ada, "N-01"))).containsExactly(LocalDate.of(2026, 3, 21));
        assertThat(dates(emails(ada, "N-02"))).containsExactly(LocalDate.of(2026, 3, 24));
        assertThat(dates(emails(ada, "N-03"))).containsExactly(LocalDate.of(2026, 3, 30));
        assertThat(dates(emails(ada, "N-04"))).containsExactly(LocalDate.of(2026, 4, 13));
        assertThat(dates(emails(ada, "N-05"))).containsExactly(LocalDate.of(2026, 5, 4));
        assertThat(fx.reload(copy).getStatus().name()).isEqualTo("LOST");
    }

    @Test
    @DisplayName("AC-NOT-02 after the return no further overdue notices are sent")
    void returnedBeforeThirdNotice() {
        fx.today(LocalDate.of(2026, 3, 2));
        Member ada = fx.member(1);
        Copy copy = checkoutOnMarch2(ada);
        fx.processThrough(LocalDate.of(2026, 3, 24));
        assertThat(emails(ada, "N-01")).hasSize(1);
        assertThat(emails(ada, "N-02")).hasSize(1);

        fx.today(LocalDate.of(2026, 3, 25));
        lending.returnCopy(copy.getBarcode(), fx.librarianCen());
        fx.processThrough(LocalDate.of(2026, 5, 4));

        assertThat(emails(ada, "N-03")).isEmpty();
        assertThat(emails(ada, "N-04")).isEmpty();
        assertThat(emails(ada, "N-05")).isEmpty();
    }

    @Test
    @DisplayName("AC-NOT-03 notifications for a YOUTH member go to the guardian and never by SMS (seed S-03)")
    void youthNotifications() {
        Member cleo = fx.member(3);
        Member dan = fx.member(4);
        memberService.updatePreferences(cleo.getId(), new PreferencesRequest(cleo.getEmail(), cleo.getPhone(), true, false));
        fx.processThrough(LocalDate.of(2026, 3, 28)); // her loans are due on 2026-03-30: N-01

        List<Notification> sent = fx.notificationsOf(cleo, "N-01");
        assertThat(sent).isNotEmpty();
        assertThat(sent).allSatisfy(n -> {
            assertThat(n.getChannel()).isEqualTo(NotificationChannel.EMAIL);
            assertThat(n.getRecipient()).isEqualTo(dan.getEmail());
        });
    }

    @Test
    @DisplayName("AC-NOT-04 with SMS opt-in N-01 goes by e-mail and SMS; N-02 by e-mail only")
    void smsOnlyForDueReminder() {
        fx.today(LocalDate.of(2026, 3, 2));
        Member ada = fx.member(1);
        memberService.updatePreferences(ada.getId(), new PreferencesRequest(ada.getEmail(), ada.getPhone(), true, false));
        checkoutOnMarch2(ada);
        fx.processThrough(LocalDate.of(2026, 3, 24));

        assertThat(fx.notificationsOf(ada, "N-01")).extracting(Notification::getChannel)
                .containsExactlyInAnyOrder(NotificationChannel.EMAIL, NotificationChannel.SMS);
        assertThat(fx.notificationsOf(ada, "N-02")).extracting(Notification::getChannel)
                .containsExactly(NotificationChannel.EMAIL);
    }

    @Test
    @DisplayName("AC-NOT-05 two due reminders on the same day become one combined message")
    void combinedMessage() {
        fx.today(LocalDate.of(2026, 3, 2));
        Member ada = fx.member(1);
        checkoutOnMarch2(ada);
        checkoutOnMarch2(ada);
        fx.processThrough(LocalDate.of(2026, 3, 21));

        List<Notification> emails = emails(ada, "N-01");
        assertThat(emails).hasSize(1);
        assertThat(emails.get(0).getBody()).contains("Test title").contains("is due on 2026-03-23");
        assertThat(emails.get(0).getBody().split("Test title", -1).length - 1).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("F-NOT-02 N-09 is sent 30 days before the membership ends (seed: valid until 2026-09-30)")
    void membershipReminder() {
        Member ada = fx.member(1);
        fx.today(LocalDate.of(2026, 8, 30));
        fx.processThrough(LocalDate.of(2026, 8, 30));
        assertThat(fx.notificationsOf(ada, "N-09")).isEmpty();
        fx.processThrough(LocalDate.of(2026, 8, 31));
        assertThat(dates(fx.notificationsOf(ada, "N-09"))).containsExactly(LocalDate.of(2026, 8, 31));
    }
}
