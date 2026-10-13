package be.npl.shelfwise.common;

/**
 * Thrown when a request violates a business rule. The {@code ruleId} refers to the BR-/F- id of the
 * specification so that refusals can be traced back to a requirement.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;
    private final String ruleId;

    public BusinessRuleException(String code, String ruleId, String message) {
        super(message);
        this.code = code;
        this.ruleId = ruleId;
    }

    public String getCode() {
        return code;
    }

    public String getRuleId() {
        return ruleId;
    }
}
