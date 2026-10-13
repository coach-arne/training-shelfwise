package be.npl.shelfwise.catalogue.web;

import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CatalogueDtos {

    private CatalogueDtos() {
    }

    public record BranchView(Long id, String code, String name) {
    }

    public record BranchAvailability(Long branchId, String branchCode, String branchName, long available) {
    }

    public record CopyView(Long id, String barcode, Long titleId, String title, Long homeBranchId,
                           String homeBranchCode, Long currentBranchId, String currentBranchCode,
                           String destinationBranchCode, String shelfMark, CopyStatus status,
                           BigDecimal replacementCost, BigDecimal effectiveReplacementCost,
                           LocalDate acquisitionDate) {
    }

    /** A search result. {@code copies} is only filled for staff (F-CAT-04). */
    public record TitleResult(Long id, String title, String authors, String publisher, Integer publicationYear,
                              String isbn, MaterialType materialType, List<BranchAvailability> availability,
                              long waitingReservations, List<CopyView> copies) {
    }

    public record TitleRequest(@NotBlank String title, @NotBlank String authors, String publisher,
                               Integer publicationYear, String isbn, @NotNull MaterialType materialType) {
    }

    public record CopyRequest(@NotNull Long titleId, String barcode, @NotNull Long homeBranchId, String shelfMark,
                              BigDecimal replacementCost, LocalDate acquisitionDate) {
    }

    public record CopyUpdateRequest(@NotNull Long homeBranchId, String shelfMark, BigDecimal replacementCost) {
    }

    public record CopyStatusRequest(@NotNull CopyStatus status) {
    }
}
