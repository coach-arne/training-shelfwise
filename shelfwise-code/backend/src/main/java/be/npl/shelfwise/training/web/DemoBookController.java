package be.npl.shelfwise.training.web;

import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.catalogue.entity.Title;
import be.npl.shelfwise.catalogue.repository.CopyRepository;
import be.npl.shelfwise.catalogue.repository.TitleRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A training aid, not a Shelfwise feature: lists the example books of the seed catalogue with their copies,
 * so trainees know what can be searched, checked out or returned. Training mode only.
 */
@RestController
@RequestMapping("/api/demo")
@ConditionalOnProperty(name = "shelfwise.training-mode", havingValue = "true")
@RequiredArgsConstructor
public class DemoBookController {

    private final TitleRepository titles;
    private final CopyRepository copies;

    public record DemoCopy(String barcode, CopyStatus status, String currentBranchCode) {
    }

    public record DemoBook(Long titleId, String title, String authors, String isbn, MaterialType materialType,
                           List<DemoCopy> copies) {
    }

    @GetMapping("/books")
    @Transactional(readOnly = true)
    public List<DemoBook> books() {
        List<Title> all = titles.findAll(Sort.by("title"));
        Map<Long, List<Copy>> copiesByTitle = copies.findAll().stream()
                .collect(Collectors.groupingBy(copy -> copy.getTitle().getId()));
        return all.stream().map(title -> toBook(title, copiesByTitle.getOrDefault(title.getId(), List.of()))).toList();
    }

    private static DemoBook toBook(Title title, List<Copy> copies) {
        List<DemoCopy> demoCopies = copies.stream()
                .sorted(Comparator.comparing(Copy::getBarcode))
                .map(copy -> new DemoCopy(copy.getBarcode(), copy.getStatus(), copy.getCurrentBranch().getCode()))
                .toList();
        return new DemoBook(title.getId(), title.getTitle(), title.getAuthors(), title.getIsbn(), title.getMaterialType(),
                demoCopies);
    }
}
