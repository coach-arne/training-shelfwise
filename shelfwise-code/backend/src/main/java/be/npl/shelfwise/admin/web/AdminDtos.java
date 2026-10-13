package be.npl.shelfwise.admin.web;

import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.member.entity.MemberCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record FineRateView(Long id, MemberCategory category, MaterialType materialType, BigDecimal dailyRate,
                               LocalDate validFrom, boolean editable) {
    }

    public record FineRateRequest(@NotNull MemberCategory category, @NotNull MaterialType materialType,
                                  @NotNull BigDecimal dailyRate, @NotNull LocalDate validFrom) {
    }

    public record HolidayView(Long id, LocalDate date, String name) {
    }

    public record HolidayRequest(@NotNull LocalDate date, @NotBlank String name) {
    }
}
