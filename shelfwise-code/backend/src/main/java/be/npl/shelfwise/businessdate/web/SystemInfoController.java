package be.npl.shelfwise.businessdate.web;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public information shown in every screen: the business date and whether training mode is on (F-TRN-01). */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemInfoController {

    private final BusinessDateService businessDate;

    public record SystemInfo(LocalDate businessDate, boolean trainingMode) {
    }

    @GetMapping("/info")
    public SystemInfo info() {
        return new SystemInfo(businessDate.today(), businessDate.isTrainingMode());
    }
}
