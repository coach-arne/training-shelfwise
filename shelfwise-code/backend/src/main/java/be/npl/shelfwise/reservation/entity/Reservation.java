package be.npl.shelfwise.reservation.entity;

import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.Title;
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

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Title title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Branch pickupBranch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    /** Queue position is determined by this timestamp (F-RES-03). */
    @Column(nullable = false)
    private Instant placedAt;

    /** The copy that is on hold, or in transit, for this reservation. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Copy assignedCopy;

    /** Date of the pickup notification N-06. */
    private LocalDate notifiedOn;

    /** Last hold day (BR-RES-02, BR-RES-06). */
    private LocalDate holdUntil;

    @Column(nullable = false)
    private boolean reminderSent;

    private LocalDate endedOn;

    @Column(nullable = false)
    private boolean cancelledByLibrary;
}
