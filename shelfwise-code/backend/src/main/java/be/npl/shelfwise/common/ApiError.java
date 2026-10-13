package be.npl.shelfwise.common;

import java.util.List;

/** Error payload returned by every failing API call. */
public record ApiError(String code, String ruleId, String message, List<String> details) {

    public static ApiError of(String code, String ruleId, String message) {
        return new ApiError(code, ruleId, message, List.of());
    }
}
