package be.npl.shelfwise.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The authenticated caller. For members {@code accountId} is the member id; for staff it is the staff account id.
 * Members only ever act on their own data: services take the member id from here, never from the request.
 */
public record CurrentUser(Long accountId, Role role, Long branchId) {

    public static final String CLAIM_UID = "uid";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_BRANCH = "branch";

    public static CurrentUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Not authenticated");
        }
        Number uid = jwt.getClaim(CLAIM_UID);
        Number branch = jwt.getClaim(CLAIM_BRANCH);
        return new CurrentUser(uid.longValue(), Role.valueOf(jwt.getClaimAsString(CLAIM_ROLE)),
                branch == null ? null : branch.longValue());
    }

    /** The current user when logged in, or null for anonymous visitors. */
    public static CurrentUser getOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            return null;
        }
        return get();
    }

    public boolean isMember() {
        return role == Role.MEMBER;
    }

    public boolean isStaff() {
        return role.isStaff();
    }
}
