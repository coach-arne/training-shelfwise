package be.npl.shelfwise.catalogue.entity;

import java.math.BigDecimal;

/** BOOK, AUDIOVISUAL or MAGAZINE (F-CAT-02): loan period and default replacement cost are fixed. */
public enum MaterialType {
    BOOK(21, "25.00"),
    AUDIOVISUAL(7, "20.00"),
    MAGAZINE(14, "6.00");

    private final int loanPeriodDays;
    private final BigDecimal defaultReplacementCost;

    MaterialType(int loanPeriodDays, String defaultReplacementCost) {
        this.loanPeriodDays = loanPeriodDays;
        this.defaultReplacementCost = new BigDecimal(defaultReplacementCost);
    }

    public int loanPeriodDays() {
        return loanPeriodDays;
    }

    public BigDecimal defaultReplacementCost() {
        return defaultReplacementCost;
    }
}
