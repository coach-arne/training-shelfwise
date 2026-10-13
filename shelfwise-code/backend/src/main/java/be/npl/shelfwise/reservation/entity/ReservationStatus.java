package be.npl.shelfwise.reservation.entity;

public enum ReservationStatus {
    WAITING,
    ON_HOLD,
    COLLECTED,
    EXPIRED,
    CANCELLED;

    public boolean isActive() {
        return this == WAITING || this == ON_HOLD;
    }
}
