package be.npl.shelfwise.training.service;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.notification.entity.Notification;
import be.npl.shelfwise.notification.repository.NotificationRepository;
import be.npl.shelfwise.processing.DailyProcessingService;
import be.npl.shelfwise.processing.DailyProcessingSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Training support functions (section 15). Only available in training mode. */
@Service
@ConditionalOnProperty(name = "shelfwise.training-mode", havingValue = "true")
@RequiredArgsConstructor
public class TrainingService {

    private final BusinessDateService businessDate;
    private final DailyProcessingService processing;
    private final SeedResetService seedReset;
    private final NotificationRepository notifications;

    public record TrainingState(LocalDate businessDate, LocalDate lastProcessedDate) {
    }

    public record OutboxEntry(Long id, String type, String channel, String recipient, String memberName,
                              String subject, String body, LocalDate date, String deliveryStatus, Instant createdAt) {
    }

    public TrainingState state() {
        return new TrainingState(businessDate.today(), businessDate.lastProcessedDate());
    }

    /**
     * F-TRN-01: moving forward runs the missing daily processing runs in date order. The day that is left has been
     * processed (it ran after closing time). Moving backward is only possible together with a reset (F-TRN-03).
     */
    public DailyProcessingSummary setBusinessDate(LocalDate target) {
        LocalDate current = businessDate.today();
        if (target.isBefore(current)) {
            throw new BusinessRuleException("BACKWARD_NOT_ALLOWED", "F-TRN-01",
                    "The business date can only move backward together with a reset to seed data.");
        }
        DailyProcessingSummary summary = processing.catchUp(target.minusDays(1));
        businessDate.setBusinessDate(target);
        return summary;
    }

    /** F-TRN-02: run the daily processing of the current business date now. */
    public DailyProcessingSummary runDailyProcessingNow() {
        return processing.catchUp(businessDate.today());
    }

    /** F-TRN-03 */
    public void reset() {
        seedReset.reset();
    }

    /** F-TRN-04 */
    @Transactional(readOnly = true)
    public List<OutboxEntry> outbox(int limit) {
        return notifications.findAllByOrderByIdDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 500))).stream()
                .map(this::toEntry).toList();
    }

    private OutboxEntry toEntry(Notification n) {
        return new OutboxEntry(n.getId(), n.getType(), n.getChannel().name(), n.getRecipient(),
                n.getMember() == null ? null : n.getMember().fullName(), n.getSubject(), n.getBody(),
                n.getNotificationDate(), n.getDeliveryStatus(), n.getCreatedAt());
    }
}
