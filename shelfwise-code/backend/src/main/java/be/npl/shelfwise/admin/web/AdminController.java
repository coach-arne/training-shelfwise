package be.npl.shelfwise.admin.web;

import be.npl.shelfwise.admin.service.FineRateAdminService;
import be.npl.shelfwise.admin.service.HolidayAdminService;
import be.npl.shelfwise.admin.web.AdminDtos.FineRateRequest;
import be.npl.shelfwise.admin.web.AdminDtos.FineRateView;
import be.npl.shelfwise.admin.web.AdminDtos.HolidayRequest;
import be.npl.shelfwise.admin.web.AdminDtos.HolidayView;
import be.npl.shelfwise.security.Roles;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Maintain fine rates and public holidays: administrators only (section 3.2). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize(Roles.ADMIN)
@RequiredArgsConstructor
public class AdminController {

    private final FineRateAdminService fineRates;
    private final HolidayAdminService holidays;

    @GetMapping("/fine-rates")
    public List<FineRateView> fineRates() {
        return fineRates.list();
    }

    @PostMapping("/fine-rates")
    public FineRateView addFineRate(@Valid @RequestBody FineRateRequest request) {
        return fineRates.create(request);
    }

    @DeleteMapping("/fine-rates/{id}")
    public void deleteFineRate(@PathVariable Long id) {
        fineRates.delete(id);
    }

    @GetMapping("/holidays")
    public List<HolidayView> holidays(@RequestParam int year) {
        return holidays.listForYear(year);
    }

    @PostMapping("/holidays")
    public HolidayView addHoliday(@Valid @RequestBody HolidayRequest request) {
        return holidays.add(request);
    }

    @DeleteMapping("/holidays/{id}")
    public void removeHoliday(@PathVariable Long id) {
        holidays.remove(id);
    }
}
