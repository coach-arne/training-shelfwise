package be.npl.shelfwise.catalogue.web;

import be.npl.shelfwise.catalogue.service.CatalogueService;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.CopyRequest;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.CopyStatusRequest;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.CopyUpdateRequest;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.CopyView;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.TitleRequest;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.TitleResult;
import be.npl.shelfwise.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Maintain titles and copies: librarians and branch managers (section 3.2). */
@RestController
@RequestMapping("/api/staff/catalogue")
@PreAuthorize(Roles.DESK)
@RequiredArgsConstructor
public class StaffCatalogueController {

    private final CatalogueService catalogue;

    @PostMapping("/titles")
    public TitleResult createTitle(@Valid @RequestBody TitleRequest request) {
        return catalogue.createTitle(request);
    }

    @PutMapping("/titles/{id}")
    public TitleResult updateTitle(@PathVariable Long id, @Valid @RequestBody TitleRequest request) {
        return catalogue.updateTitle(id, request);
    }

    @PostMapping("/copies")
    public CopyView createCopy(@Valid @RequestBody CopyRequest request) {
        return catalogue.createCopy(request);
    }

    @PutMapping("/copies/{id}")
    public CopyView updateCopy(@PathVariable Long id, @Valid @RequestBody CopyUpdateRequest request) {
        return catalogue.updateCopy(id, request);
    }

    @PostMapping("/copies/{id}/status")
    public CopyView changeStatus(@PathVariable Long id, @Valid @RequestBody CopyStatusRequest request) {
        return catalogue.changeStatus(id, request.status());
    }

    @GetMapping("/copies/barcode/{barcode}")
    public CopyView copyByBarcode(@PathVariable String barcode) {
        return catalogue.getCopyByBarcode(barcode);
    }
}
