package be.npl.shelfwise.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.npl.shelfwise.common.BusinessRuleException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.security.access.AccessDeniedException;

/** Assertions for refused actions. */
public final class Rules {

    private Rules() {
    }

    /** The call is refused by a business rule with the given error code. */
    public static void assertRefused(String code, ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode()).isEqualTo(code);
    }

    /** The call is refused because of the role or ownership of the caller. */
    public static void assertForbidden(ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
    }
}
