package be.npl.shelfwise.catalogue.service;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.entity.Title;
import be.npl.shelfwise.catalogue.repository.BranchRepository;
import be.npl.shelfwise.catalogue.repository.CopyRepository;
import be.npl.shelfwise.catalogue.repository.TitleRepository;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.BranchAvailability;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.BranchView;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.CopyRequest;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.CopyUpdateRequest;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.CopyView;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.TitleRequest;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.TitleResult;
import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.common.NotFoundException;
import be.npl.shelfwise.lending.service.CopyRoutingService;
import be.npl.shelfwise.reservation.entity.ReservationStatus;
import be.npl.shelfwise.reservation.repository.ReservationRepository;
import be.npl.shelfwise.reservation.service.ReservationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Titles, copies, copy statuses, search and availability (F-CAT-01 to F-CAT-04). */
@Service
@RequiredArgsConstructor
@Transactional
public class CatalogueService {

    public enum SearchType {
        ALL, TITLE, AUTHOR, ISBN
    }

    private static final int MAX_RESULTS = 50;

    private final TitleRepository titles;
    private final CopyRepository copies;
    private final BranchRepository branches;
    private final ReservationRepository reservations;
    private final CopyRoutingService routing;
    private final ReservationService reservationService;
    private final BusinessDateService businessDate;

    // ----- search -----

    @Transactional(readOnly = true)
    public List<BranchView> branches() {
        return branches.findAllByOrderByCode().stream().map(b -> new BranchView(b.getId(), b.getCode(), b.getName())).toList();
    }

    /**
     * Search by title, author, ISBN or keyword. Staff can also search by barcode and see the status and current
     * branch of every copy.
     */
    @Transactional(readOnly = true)
    public List<TitleResult> search(String query, SearchType type, boolean staff) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.trim();
        List<Title> found = new ArrayList<>();
        if (staff) {
            copies.findByBarcode(q).ifPresent(c -> found.add(c.getTitle()));
        }
        if (found.isEmpty()) {
            String pattern = "%" + q.toLowerCase() + "%";
            var page = PageRequest.of(0, MAX_RESULTS);
            found.addAll(switch (type) {
                case TITLE -> titles.searchByTitle(pattern, page);
                case AUTHOR -> titles.searchByAuthor(pattern, page);
                case ISBN -> titles.searchByIsbn("%" + q.replace("-", "") + "%", page);
                case ALL -> titles.searchKeyword(pattern, page);
            });
        }
        return toResults(found, staff);
    }

    @Transactional(readOnly = true)
    public TitleResult getTitle(Long id, boolean staff) {
        Title title = titles.findById(id).orElseThrow(() -> new NotFoundException("Title", id));
        return toResults(List.of(title), staff).get(0);
    }

    @Transactional(readOnly = true)
    public CopyView getCopyByBarcode(String barcode) {
        return toCopyView(copies.findByBarcode(barcode.trim()).orElseThrow(() -> new NotFoundException("Copy", barcode)));
    }

    // ----- maintain titles -----

    public TitleResult createTitle(TitleRequest request) {
        Title title = new Title();
        applyTitle(title, request);
        return toResults(List.of(titles.save(title)), true).get(0);
    }

    public TitleResult updateTitle(Long id, TitleRequest request) {
        Title title = titles.findById(id).orElseThrow(() -> new NotFoundException("Title", id));
        if (title.getMaterialType() != request.materialType() && copies.countByTitleId(id) > 0) {
            throw new BusinessRuleException("MATERIAL_TYPE_LOCKED", "F-CAT-01",
                    "The material type cannot change once the title has copies.");
        }
        applyTitle(title, request);
        return toResults(List.of(titles.save(title)), true).get(0);
    }

    // ----- maintain copies -----

    /** A new copy is AVAILABLE at its home branch; a waiting reservation gets it first (BR-RES-07). */
    public CopyView createCopy(CopyRequest request) {
        Title title = titles.findById(request.titleId()).orElseThrow(() -> new NotFoundException("Title", request.titleId()));
        Branch home = branches.findById(request.homeBranchId())
                .orElseThrow(() -> new NotFoundException("Branch", request.homeBranchId()));
        LocalDate today = businessDate.today();
        Copy copy = new Copy();
        copy.setTitle(title);
        copy.setBarcode(barcodeFor(request.barcode()));
        copy.setHomeBranch(home);
        copy.setCurrentBranch(home);
        copy.setShelfMark(request.shelfMark());
        copy.setReplacementCost(request.replacementCost());
        copy.setAcquisitionDate(request.acquisitionDate() != null ? request.acquisitionDate() : today);
        copy.setStatus(CopyStatus.AVAILABLE);
        copies.save(copy);
        routing.offerToQueue(copy, today);
        return toCopyView(copy);
    }

    public CopyView updateCopy(Long id, CopyUpdateRequest request) {
        Copy copy = copies.findById(id).orElseThrow(() -> new NotFoundException("Copy", id));
        copy.setHomeBranch(branches.findById(request.homeBranchId())
                .orElseThrow(() -> new NotFoundException("Branch", request.homeBranchId())));
        copy.setShelfMark(request.shelfMark());
        copy.setReplacementCost(request.replacementCost());
        return toCopyView(copies.save(copy));
    }

    /**
     * Manual status change by staff (BR-CAT-01): AVAILABLE to DAMAGED or WITHDRAWN, DAMAGED to AVAILABLE or WITHDRAWN.
     * A copy that is on loan, on hold or in transit cannot be withdrawn directly.
     */
    public CopyView changeStatus(Long id, CopyStatus target) {
        Copy copy = copies.findById(id).orElseThrow(() -> new NotFoundException("Copy", id));
        LocalDate today = businessDate.today();
        if (!isManualTransition(copy.getStatus(), target)) {
            throw new BusinessRuleException("INVALID_COPY_TRANSITION", "BR-CAT-01",
                    "A copy that is " + copy.getStatus() + " cannot be set to " + target
                            + ". Only AVAILABLE or DAMAGED copies can be damaged, repaired or withdrawn.");
        }
        copy.setStatus(target);
        copies.save(copy);
        if (target == CopyStatus.AVAILABLE) {
            routing.offerToQueue(copy, today);
        } else if (target == CopyStatus.WITHDRAWN) {
            Long titleId = copy.getTitle().getId();
            if (copies.countByTitleIdAndStatusNotIn(titleId, Set.of(CopyStatus.WITHDRAWN, CopyStatus.LOST)) == 0) {
                reservationService.cancelAllActiveForTitle(titleId, today);
            }
        }
        return toCopyView(copy);
    }

    // ----- mapping -----

    private List<TitleResult> toResults(List<Title> found, boolean staff) {
        if (found.isEmpty()) {
            return List.of();
        }
        List<Long> ids = found.stream().map(Title::getId).toList();
        List<Branch> branchList = branches.findAllByOrderByCode();
        Map<String, Long> available = new HashMap<>();
        for (Object[] row : copies.availabilityRows(ids)) {
            available.put(row[0] + ":" + row[1], (Long) row[2]);
        }
        List<TitleResult> results = new ArrayList<>();
        for (Title t : found) {
            List<BranchAvailability> availability = branchList.stream()
                    .map(b -> new BranchAvailability(b.getId(), b.getCode(), b.getName(),
                            available.getOrDefault(t.getId() + ":" + b.getId(), 0L)))
                    .toList();
            long waiting = reservations.countByTitleIdAndStatus(t.getId(), ReservationStatus.WAITING);
            List<CopyView> copyViews = staff
                    ? copies.findByTitleIdOrderByBarcode(t.getId()).stream().map(this::toCopyView).toList() : null;
            results.add(new TitleResult(t.getId(), t.getTitle(), t.getAuthors(), t.getPublisher(),
                    t.getPublicationYear(), t.getIsbn(), t.getMaterialType(), availability, waiting, copyViews));
        }
        return results;
    }

    public CopyView toCopyView(Copy c) {
        Optional<Branch> destination = Optional.ofNullable(c.getTransitDestination());
        return new CopyView(c.getId(), c.getBarcode(), c.getTitle().getId(), c.getTitle().getTitle(),
                c.getHomeBranch().getId(), c.getHomeBranch().getCode(), c.getCurrentBranch().getId(),
                c.getCurrentBranch().getCode(), destination.map(Branch::getCode).orElse(null), c.getShelfMark(),
                c.getStatus(), c.getReplacementCost(), c.effectiveReplacementCost(), c.getAcquisitionDate());
    }

    // ----- helpers -----

    private void applyTitle(Title title, TitleRequest request) {
        title.setTitle(request.title().trim());
        title.setAuthors(request.authors().trim());
        title.setPublisher(request.publisher());
        title.setPublicationYear(request.publicationYear());
        title.setIsbn(request.isbn() == null || request.isbn().isBlank() ? null : request.isbn().trim());
        title.setMaterialType(request.materialType());
    }

    private String barcodeFor(String requested) {
        if (requested != null && !requested.isBlank()) {
            String barcode = requested.trim();
            if (copies.existsByBarcode(barcode)) {
                throw new BusinessRuleException("DUPLICATE_BARCODE", "F-CAT-01", "The barcode " + barcode + " is already in use.");
            }
            return barcode;
        }
        long next = copies.count() + 100001;
        while (copies.existsByBarcode("C" + next)) {
            next++;
        }
        return "C" + next;
    }

    private static boolean isManualTransition(CopyStatus from, CopyStatus to) {
        return (from == CopyStatus.AVAILABLE && (to == CopyStatus.DAMAGED || to == CopyStatus.WITHDRAWN))
                || (from == CopyStatus.DAMAGED && (to == CopyStatus.AVAILABLE || to == CopyStatus.WITHDRAWN));
    }
}
