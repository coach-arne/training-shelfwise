package be.npl.shelfwise.notification.entity;

/** Notification catalogue of F-NOT-02. */
public enum NotificationType {
    N_01("N-01", "Due date reminder"),
    N_02("N-02", "First overdue notice"),
    N_03("N-03", "Second overdue notice"),
    N_04("N-04", "Third overdue notice"),
    N_05("N-05", "Lost declaration notice"),
    N_06("N-06", "Reservation ready for pickup"),
    N_07("N-07", "Pickup reminder"),
    N_08("N-08", "Reservation expired"),
    N_09("N-09", "Membership expiry reminder"),
    N_10("N-10", "Payment receipt");

    private final String code;
    private final String title;

    NotificationType(String code, String title) {
        this.code = code;
        this.title = title;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }
}
