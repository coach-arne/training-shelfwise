package be.npl.shelfwise;

import static org.assertj.core.api.Assertions.assertThat;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.catalogue.repository.CopyRepository;
import be.npl.shelfwise.catalogue.repository.TitleRepository;
import be.npl.shelfwise.member.repository.MemberRepository;
import be.npl.shelfwise.security.StaffAccountRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SeedDataIT extends IntegrationTestBase {

    @Autowired TitleRepository titles;
    @Autowired CopyRepository copies;
    @Autowired MemberRepository members;
    @Autowired StaffAccountRepository staff;
    @Autowired BusinessDateService businessDate;

    @Test
    @DisplayName("Appendix B: seed data has the required size and business date")
    void seedSize() {
        assertThat(titles.count()).isGreaterThanOrEqualTo(30);
        assertThat(copies.count()).isGreaterThanOrEqualTo(80);
        assertThat(members.count()).isEqualTo(19);
        assertThat(staff.count()).isEqualTo(4);
        assertThat(businessDate.today()).isEqualTo(LocalDate.of(2026, 3, 16));
    }
}
