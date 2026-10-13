package be.npl.shelfwise.notification.service;

import be.npl.shelfwise.config.AppProperties;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.notification.entity.Notification;
import be.npl.shelfwise.notification.entity.NotificationChannel;
import be.npl.shelfwise.notification.entity.NotificationType;
import be.npl.shelfwise.notification.helper.NotificationTextBuilder;
import be.npl.shelfwise.notification.helper.RecipientResolver;
import be.npl.shelfwise.notification.repository.NotificationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes notifications to the outbox (F-NOT-01). Nothing is really sent in this implementation. Every notification
 * is logged with date, type, channel, recipient and delivery status.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    static final String DELIVERY_STATUS = "SENT (simulated)";

    private final NotificationRepository notifications;
    private final AppProperties properties;

    /** Immediate notifications: N-06 and N-10. N-06 is also sent by SMS to members with SMS opt-in. */
    @Transactional
    public void sendNow(NotificationType type, Member member, LocalDate date, List<String> lines) {
        String body = NotificationTextBuilder.emailBody(RecipientResolver.greetingName(member),
                NotificationTextBuilder.single(type, lines), properties.portalUrl());
        save(member, type.code(), NotificationChannel.EMAIL, RecipientResolver.emailAddress(member),
                NotificationTextBuilder.subject(List.of(type)), body, date);
        if (type == NotificationType.N_06 && RecipientResolver.receivesSms(member)) {
            sendSms(member, type, lines, date);
        }
    }

    /**
     * Scheduled notifications of one daily processing run. Drafts for the same recipient are combined into one
     * e-mail (AC-NOT-05). N-01 is also sent by SMS to members with SMS opt-in, one SMS per member (AC-NOT-04).
     *
     * @return the number of combined messages that were written to the outbox
     */
    @Transactional
    public int sendScheduled(List<NotificationDraft> drafts, LocalDate date) {
        Map<String, List<NotificationDraft>> byRecipient = new LinkedHashMap<>();
        for (NotificationDraft draft : drafts) {
            byRecipient.computeIfAbsent(RecipientResolver.emailAddress(draft.member()), k -> new ArrayList<>()).add(draft);
        }
        int messages = 0;
        for (Map.Entry<String, List<NotificationDraft>> entry : byRecipient.entrySet()) {
            List<NotificationDraft> group = entry.getValue();
            Member first = group.get(0).member();
            Map<NotificationType, List<String>> sections = new EnumMap<>(NotificationType.class);
            for (NotificationDraft draft : group) {
                sections.computeIfAbsent(draft.type(), k -> new ArrayList<>()).addAll(draft.lines());
            }
            List<NotificationType> types = new ArrayList<>(sections.keySet());
            String codes = String.join(",", types.stream().map(NotificationType::code).toList());
            String body = NotificationTextBuilder.emailBody(RecipientResolver.greetingName(first), sections,
                    properties.portalUrl());
            save(first, codes, NotificationChannel.EMAIL, entry.getKey(), NotificationTextBuilder.subject(types), body, date);
            messages++;
        }
        messages += sendDueReminderSms(drafts, date);
        return messages;
    }

    private int sendDueReminderSms(List<NotificationDraft> drafts, LocalDate date) {
        Map<Long, List<NotificationDraft>> byMember = new LinkedHashMap<>();
        for (NotificationDraft draft : drafts) {
            if (draft.type() == NotificationType.N_01 && RecipientResolver.receivesSms(draft.member())) {
                byMember.computeIfAbsent(draft.member().getId(), k -> new ArrayList<>()).add(draft);
            }
        }
        for (List<NotificationDraft> memberDrafts : byMember.values()) {
            List<String> lines = memberDrafts.stream().flatMap(d -> d.lines().stream()).toList();
            sendSms(memberDrafts.get(0).member(), NotificationType.N_01, lines, date);
        }
        return byMember.size();
    }

    private void sendSms(Member member, NotificationType type, List<String> lines, LocalDate date) {
        save(member, type.code(), NotificationChannel.SMS, member.getPhone(), type.title(),
                NotificationTextBuilder.smsBody(member.getFirstName(), lines), date);
    }

    private void save(Member member, String type, NotificationChannel channel, String recipient, String subject,
                      String body, LocalDate date) {
        Notification notification = new Notification();
        notification.setMember(member);
        notification.setType(type);
        notification.setChannel(channel);
        notification.setRecipient(recipient);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setNotificationDate(date);
        notification.setDeliveryStatus(DELIVERY_STATUS);
        notification.setCreatedAt(Instant.now());
        notifications.save(notification);
    }
}
