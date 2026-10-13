package be.npl.shelfwise.report.web;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.report.service.ReportService;
import be.npl.shelfwise.report.service.ReportService.CirculationRow;
import be.npl.shelfwise.report.service.ReportService.OverdueRow;
import be.npl.shelfwise.security.CurrentUser;
import be.npl.shelfwise.security.Roles;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** R-01 and R-02: librarians (own branch), branch managers and administrators (all branches). */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize(Roles.STAFF)
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reports;
    private final BusinessDateService businessDate;

    @GetMapping("/r01")
    public ResponseEntity<?> dailyCirculation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long branchId, @RequestParam(defaultValue = "json") String format) {
        Long branch = reports.resolveBranch(CurrentUser.get(), branchId);
        LocalDate day = date != null ? date : businessDate.today();
        List<CirculationRow> rows = reports.dailyCirculation(day, branch);
        return "csv".equalsIgnoreCase(format) ? csv("R-01-daily-circulation-" + day + ".csv", reports.circulationCsv(rows))
                : ResponseEntity.ok(rows);
    }

    @GetMapping("/r02")
    public ResponseEntity<?> overdueItems(@RequestParam(required = false) Long branchId,
                                          @RequestParam(defaultValue = "json") String format) {
        Long branch = reports.resolveBranch(CurrentUser.get(), branchId);
        List<OverdueRow> rows = reports.overdueItems(branch);
        return "csv".equalsIgnoreCase(format)
                ? csv("R-02-overdue-items-" + businessDate.today() + ".csv", reports.overdueCsv(rows))
                : ResponseEntity.ok(rows);
    }

    private static ResponseEntity<String> csv(String filename, String content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv"))
                .body(content);
    }
}
