package be.npl.shelfwise.seed;

import be.npl.shelfwise.catalogue.repository.BranchRepository;
import be.npl.shelfwise.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Loads the seed data when the database is empty (seed data is used instead of a migration, section 1.3). */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SeedOnStartup implements ApplicationRunner {

    private final BranchRepository branches;
    private final SeedDataLoader loader;
    private final AppProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (properties.seedOnEmpty() && branches.count() == 0) {
            log.info("Empty database: loading seed data");
            loader.load();
        }
    }
}
