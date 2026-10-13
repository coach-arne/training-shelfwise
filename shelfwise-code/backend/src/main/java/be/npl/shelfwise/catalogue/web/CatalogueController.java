package be.npl.shelfwise.catalogue.web;

import be.npl.shelfwise.catalogue.service.CatalogueService;
import be.npl.shelfwise.catalogue.service.CatalogueService.SearchType;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.BranchView;
import be.npl.shelfwise.catalogue.web.CatalogueDtos.TitleResult;
import be.npl.shelfwise.security.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public catalogue search with availability per branch (F-CAT-04, AC-ROL-05). Staff additionally see copies. */
@RestController
@RequestMapping("/api/catalogue")
@RequiredArgsConstructor
public class CatalogueController {

    private final CatalogueService catalogue;

    @GetMapping("/search")
    public List<TitleResult> search(@RequestParam(defaultValue = "") String q,
                                    @RequestParam(defaultValue = "ALL") SearchType type) {
        return catalogue.search(q, type, isStaff());
    }

    @GetMapping("/titles/{id}")
    public TitleResult title(@PathVariable Long id) {
        return catalogue.getTitle(id, isStaff());
    }

    @GetMapping("/branches")
    public List<BranchView> branches() {
        return catalogue.branches();
    }

    private static boolean isStaff() {
        CurrentUser user = CurrentUser.getOrNull();
        return user != null && user.isStaff();
    }
}
