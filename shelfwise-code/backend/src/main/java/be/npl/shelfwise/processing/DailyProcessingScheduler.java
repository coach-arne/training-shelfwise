package be.npl.shelfwise.processing;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.config.AppProperties;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * In production the daily processing runs by itself after closing time. At startup, runs that were missed while the
 * application was down are caught up. In training mode the administrator controls processing (F-TRN-01, F-TRN-02).
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DailyProcessingScheduler implements ApplicationRunner {

    private final DailyProcessingService processing;
    private final BusinessDateService businessDate;
    private final AppProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (isActive()) {
            LocalDate yesterday = businessDate.today().minusDays(1);
            log.info("Catching up daily processing through {}", yesterday);
            processing.catchUp(yesterday);
        }
    }

    @Scheduled(cron = "${shelfwise.daily-processing-cron:0 0 22 * * *}", zone = "${shelfwise.time-zone:Europe/Brussels}")
    public void runAfterClosing() {
        if (isActive()) {
            processing.catchUp(businessDate.today());
        }
    }

    private boolean isActive() {
        return properties.schedulerEnabled() && !properties.trainingMode();
    }
}
