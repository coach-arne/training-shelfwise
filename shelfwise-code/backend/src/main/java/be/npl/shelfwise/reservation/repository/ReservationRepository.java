package be.npl.shelfwise.reservation.repository;

import be.npl.shelfwise.reservation.entity.Reservation;
import be.npl.shelfwise.reservation.entity.ReservationStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** The queue of a title: WAITING reservations without an assigned copy, oldest first (F-RES-03). */
    List<Reservation> findByTitleIdAndStatusAndAssignedCopyIsNullOrderByPlacedAtAscIdAsc(
            Long titleId, ReservationStatus status);

    long countByTitleIdAndStatus(Long titleId, ReservationStatus status);

    List<Reservation> findByTitleIdAndStatusIn(Long titleId, Collection<ReservationStatus> statuses);

    long countByMemberIdAndStatusIn(Long memberId, Collection<ReservationStatus> statuses);

    boolean existsByMemberIdAndTitleIdAndStatusIn(Long memberId, Long titleId, Collection<ReservationStatus> statuses);

    List<Reservation> findByMemberIdOrderByPlacedAtDescIdDesc(Long memberId);

    Optional<Reservation> findFirstByAssignedCopyIdAndStatusIn(Long copyId, Collection<ReservationStatus> statuses);

    List<Reservation> findByStatusAndHoldUntilLessThanEqualOrderByIdAsc(ReservationStatus status, LocalDate date);

    List<Reservation> findByStatusAndNotifiedOnAndReminderSentFalse(ReservationStatus status, LocalDate notifiedOn);

    List<Reservation> findByStatus(ReservationStatus status);
}
