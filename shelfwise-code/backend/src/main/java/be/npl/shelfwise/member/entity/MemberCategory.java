package be.npl.shelfwise.member.entity;

import java.math.BigDecimal;

/** YOUTH, ADULT or SENIOR (F-MEM-01) with the fixed fees of BR-MEM-04 and BR-RES-03. */
public enum MemberCategory {
    YOUTH("0.00", "0.00"),
    ADULT("12.00", "0.50"),
    SENIOR("6.00", "0.50");

    private final BigDecimal membershipFee;
    private final BigDecimal reservationFee;

    MemberCategory(String membershipFee, String reservationFee) {
        this.membershipFee = new BigDecimal(membershipFee);
        this.reservationFee = new BigDecimal(reservationFee);
    }

    public BigDecimal membershipFee() {
        return membershipFee;
    }

    public BigDecimal reservationFee() {
        return reservationFee;
    }
}
