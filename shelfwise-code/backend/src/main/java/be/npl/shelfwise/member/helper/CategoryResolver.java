package be.npl.shelfwise.member.helper;

import be.npl.shelfwise.member.entity.MemberCategory;
import java.time.LocalDate;
import java.time.Period;

/** BR-MEM-01: the category follows from the age of the person: YOUTH 0-17, ADULT 18-64, SENIOR 65+. */
public final class CategoryResolver {

    private CategoryResolver() {
    }

    public static MemberCategory resolve(LocalDate birthDate, LocalDate onDate) {
        int age = Period.between(birthDate, onDate).getYears();
        if (age < 18) {
            return MemberCategory.YOUTH;
        }
        if (age < 65) {
            return MemberCategory.ADULT;
        }
        return MemberCategory.SENIOR;
    }
}
