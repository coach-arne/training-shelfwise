package be.npl.shelfwise.businessdate.service;

import be.npl.shelfwise.businessdate.entity.SystemState;
import be.npl.shelfwise.businessdate.repository.SystemStateRepository;
import be.npl.shelfwise.config.AppProperties;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The business date is "today" for every function (section 5.3). In production it equals the real date in the
 * library's time zone; in training mode it is the value stored in the system state.
 */
@Service
@RequiredArgsConstructor
public class BusinessDateService {

    private final SystemStateRepository states;
    private final AppProperties properties;

    @Transactional(readOnly = true)
    public LocalDate today() {
        if (!properties.trainingMode()) {
            return LocalDate.now(ZoneId.of(properties.timeZone()));
        }
        return state().getBusinessDate();
    }

    @Transactional(readOnly = true)
    public LocalDate lastProcessedDate() {
        return state().getLastProcessedDate();
    }

    @Transactional
    public void setBusinessDate(LocalDate date) {
        state().setBusinessDate(date);
    }

    @Transactional
    public void setLastProcessedDate(LocalDate date) {
        state().setLastProcessedDate(date);
    }

    public boolean isTrainingMode() {
        return properties.trainingMode();
    }

    private SystemState state() {
        return states.findById(SystemState.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("System state has not been initialised"));
    }
}
