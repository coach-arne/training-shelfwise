package be.npl.shelfwise.notification.service;

import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.notification.entity.NotificationType;
import java.util.List;

/** A scheduled notification that is collected during daily processing and combined per recipient. */
public record NotificationDraft(Member member, NotificationType type, List<String> lines) {
}
