package be.npl.shelfwise.notification.entity;

import be.npl.shelfwise.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A row of the notification outbox (F-NOT-01, F-TRN-04). Nothing is really sent. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    /** One or more notification codes, comma separated when several were combined. */
    @Column(nullable = false)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private LocalDate notificationDate;

    @Column(nullable = false)
    private String deliveryStatus;

    @Column(nullable = false)
    private Instant createdAt;
}
