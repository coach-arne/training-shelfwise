package be.npl.shelfwise.notification.helper;

import be.npl.shelfwise.notification.entity.NotificationType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds the plain text of notifications (F-NOT-02 content: name, titles, dates, amounts, portal link). */
public final class NotificationTextBuilder {

    private NotificationTextBuilder() {
    }

    public static String subject(List<NotificationType> types) {
        return types.stream().map(NotificationType::title).distinct().collect(Collectors.joining(" / "));
    }

    /** Sections are grouped per notification type; several notices for the same day form one message. */
    public static String emailBody(String greetingName, Map<NotificationType, List<String>> sections, String portalUrl) {
        StringBuilder body = new StringBuilder("Dear ").append(greetingName).append(",\n\n");
        sections.forEach((type, lines) -> {
            body.append(type.code()).append(" - ").append(type.title()).append('\n');
            lines.forEach(line -> body.append("  - ").append(line).append('\n'));
            body.append('\n');
        });
        body.append("You can see all details in the member portal: ").append(portalUrl).append("\n\n")
                .append("Northwick Public Library");
        return body.toString();
    }

    public static String smsBody(String firstName, List<String> lines) {
        return "NPL: " + firstName + ", " + String.join(" ", lines);
    }

    public static Map<NotificationType, List<String>> single(NotificationType type, List<String> lines) {
        Map<NotificationType, List<String>> sections = new LinkedHashMap<>();
        sections.put(type, lines);
        return sections;
    }
}
