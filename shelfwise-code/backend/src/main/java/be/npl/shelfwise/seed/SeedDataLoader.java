package be.npl.shelfwise.seed;

import be.npl.shelfwise.businessdate.entity.SystemState;
import be.npl.shelfwise.businessdate.repository.SystemStateRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the seed data of Appendix B: the situation at the start of business date Monday 2026-03-16. The daily
 * processing of that date has not run yet, and the notification outbox starts empty.
 */
@Component
@RequiredArgsConstructor
public class SeedDataLoader {

    public static final LocalDate SEED_BUSINESS_DATE = LocalDate.of(2026, 3, 16);

    private final SystemStateRepository systemStates;
    private final SeedCatalogueLoader catalogueLoader;
    private final SeedScenarioLoader scenarioLoader;

    @Transactional
    public void load() {
        SystemState state = new SystemState();
        state.setBusinessDate(SEED_BUSINESS_DATE);
        state.setLastProcessedDate(SEED_BUSINESS_DATE.minusDays(1));
        systemStates.save(state);

        SeedCatalogue catalogue = catalogueLoader.load();
        scenarioLoader.load(catalogue);
    }
}
