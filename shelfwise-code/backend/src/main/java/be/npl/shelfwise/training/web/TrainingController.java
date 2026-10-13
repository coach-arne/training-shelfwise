package be.npl.shelfwise.training.web;

import be.npl.shelfwise.processing.DailyProcessingSummary;
import be.npl.shelfwise.security.Roles;
import be.npl.shelfwise.training.service.TrainingService;
import be.npl.shelfwise.training.service.TrainingService.OutboxEntry;
import be.npl.shelfwise.training.service.TrainingService.TrainingState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** F-TRN-01 to F-TRN-04: only in training mode and only for the ADMINISTRATOR role. */
@RestController
@RequestMapping("/api/training")
@PreAuthorize(Roles.ADMIN)
@ConditionalOnProperty(name = "shelfwise.training-mode", havingValue = "true")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService training;

    public record SetDateRequest(@NotNull LocalDate date) {
    }

    @GetMapping("/state")
    public TrainingState state() {
        return training.state();
    }

    @PostMapping("/business-date")
    public DailyProcessingSummary setBusinessDate(@Valid @RequestBody SetDateRequest request) {
        return training.setBusinessDate(request.date());
    }

    @PostMapping("/daily-processing")
    public DailyProcessingSummary runDailyProcessing() {
        return training.runDailyProcessingNow();
    }

    @PostMapping("/reset")
    public TrainingState reset() {
        training.reset();
        return training.state();
    }

    @GetMapping("/outbox")
    public List<OutboxEntry> outbox(@RequestParam(defaultValue = "100") int limit) {
        return training.outbox(limit);
    }
}
