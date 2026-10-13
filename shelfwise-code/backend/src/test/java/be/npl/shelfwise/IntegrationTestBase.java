package be.npl.shelfwise;

import be.npl.shelfwise.support.TestFixtures;
import be.npl.shelfwise.training.service.SeedResetService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class of the integration tests. They run against a real PostgreSQL: a Testcontainers container, or the
 * database named in the environment variable TEST_DB_URL (used when the tests run inside a container).
 * Every test starts from the seed data of Appendix B (business date Monday 2026-03-16).
 */
@SpringBootTest
@Import(TestFixtures.class)
public abstract class IntegrationTestBase {

    private static PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("TEST_DB_URL");
        if (url != null) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("TEST_DB_USER", "shelfwise"));
            registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", "shelfwise"));
        } else {
            synchronized (IntegrationTestBase.class) {
                if (postgres == null) {
                    postgres = new PostgreSQLContainer<>("postgres:16-alpine");
                    postgres.start();
                }
            }
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
        registry.add("shelfwise.training-mode", () -> "true");
        registry.add("shelfwise.scheduler-enabled", () -> "false");
        registry.add("shelfwise.jwt-secret", () -> "test-secret-test-secret-test-secret-0123456789");
    }

    @Autowired
    protected TestFixtures fx;

    @Autowired
    private SeedResetService seedReset;

    @BeforeEach
    void resetToSeed() {
        seedReset.reset();
    }
}
