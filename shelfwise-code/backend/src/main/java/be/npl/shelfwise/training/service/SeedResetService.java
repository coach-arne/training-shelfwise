package be.npl.shelfwise.training.service;

import be.npl.shelfwise.seed.SeedDataLoader;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-TRN-03: wipes all data and loads the seed data again, including the business date of the seed. */
@Service
@ConditionalOnProperty(name = "shelfwise.training-mode", havingValue = "true")
@RequiredArgsConstructor
public class SeedResetService {

    private static final String ALL_TABLES = String.join(", ",
            "notification", "online_payment_session_charge", "online_payment_session", "payment_allocation",
            "payment", "waiver", "charge_transaction", "charge", "membership_fee_payment", "reservation",
            "loan_event", "loan", "copy", "title", "staff_account", "member", "fine_rate", "public_holiday",
            "loan_limit", "branch", "system_state");

    private final EntityManager entityManager;
    private final SeedDataLoader loader;

    @Transactional
    public void reset() {
        entityManager.createNativeQuery("truncate table " + ALL_TABLES + " restart identity cascade").executeUpdate();
        entityManager.createNativeQuery("alter sequence card_number_seq restart with 1").executeUpdate();
        entityManager.clear();
        loader.load();
    }
}
