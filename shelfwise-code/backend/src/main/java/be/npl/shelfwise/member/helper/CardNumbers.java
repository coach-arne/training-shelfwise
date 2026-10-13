package be.npl.shelfwise.member.helper;

public final class CardNumbers {

    private CardNumbers() {
    }

    public static String format(long sequence) {
        return String.format("NPL%06d", sequence);
    }
}
