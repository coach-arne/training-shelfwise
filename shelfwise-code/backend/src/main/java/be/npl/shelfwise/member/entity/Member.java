package be.npl.shelfwise.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String cardNumber;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(nullable = false)
    private boolean smsOptIn;

    @Column(nullable = false)
    private boolean keepHistory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberCategory category;

    @Column(nullable = false)
    private LocalDate registrationDate;

    @Column(nullable = false)
    private LocalDate membershipStart;

    /** Last day on which the membership is valid (inclusive). */
    @Column(nullable = false)
    private LocalDate membershipEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member guardianMember;

    private String guardianName;

    private String guardianEmail;

    @Column(nullable = false)
    private String passwordHash;

    public String fullName() {
        return firstName + " " + lastName;
    }
}
