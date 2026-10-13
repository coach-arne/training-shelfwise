package be.npl.shelfwise.security;

/** Expressions for {@code @PreAuthorize}; they implement the permission matrix of section 3.2. */
public final class Roles {

    /** Lending and financial desk work: librarians and branch managers (never administrators). */
    public static final String DESK = "hasAnyRole('LIBRARIAN','BRANCH_MANAGER')";
    public static final String STAFF = "hasAnyRole('LIBRARIAN','BRANCH_MANAGER','ADMINISTRATOR')";
    public static final String MEMBER = "hasRole('MEMBER')";
    public static final String ADMIN = "hasRole('ADMINISTRATOR')";
    public static final String MANAGEMENT = "hasAnyRole('BRANCH_MANAGER','ADMINISTRATOR')";

    private Roles() {
    }
}
