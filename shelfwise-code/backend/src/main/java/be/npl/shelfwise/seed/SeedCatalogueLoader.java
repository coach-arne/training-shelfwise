package be.npl.shelfwise.seed;

import be.npl.shelfwise.calendar.entity.PublicHoliday;
import be.npl.shelfwise.calendar.repository.PublicHolidayRepository;
import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.catalogue.entity.Title;
import be.npl.shelfwise.catalogue.repository.BranchRepository;
import be.npl.shelfwise.catalogue.repository.CopyRepository;
import be.npl.shelfwise.catalogue.repository.TitleRepository;
import be.npl.shelfwise.finance.entity.FineRate;
import be.npl.shelfwise.finance.repository.FineRateRepository;
import be.npl.shelfwise.member.entity.LoanLimit;
import be.npl.shelfwise.member.entity.MemberCategory;
import be.npl.shelfwise.member.repository.LoanLimitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Seeds branches, calendar, rates, loan limits, titles and copies (Appendix B.2). */
@Component
@RequiredArgsConstructor
public class SeedCatalogueLoader {

    private static final LocalDate ACQUIRED = LocalDate.of(2025, 1, 6);
    private static final LocalDate RATES_VALID_FROM = LocalDate.of(2026, 1, 1);

    private static final List<String[]> BRANCHES = List.of(
            new String[] {"CEN", "Central"}, new String[] {"RIV", "Riverside"}, new String[] {"EAS", "Eastgate"},
            new String[] {"MIL", "Millbrook"}, new String[] {"HAR", "Harbour"}, new String[] {"WES", "Westfield"});

    private static final Map<LocalDate, String> HOLIDAYS_2026 = new LinkedHashMap<>();

    static {
        HOLIDAYS_2026.put(LocalDate.of(2026, 1, 1), "New Year's Day");
        HOLIDAYS_2026.put(LocalDate.of(2026, 4, 6), "Easter Monday");
        HOLIDAYS_2026.put(LocalDate.of(2026, 5, 1), "Labour Day");
        HOLIDAYS_2026.put(LocalDate.of(2026, 5, 14), "Ascension Day");
        HOLIDAYS_2026.put(LocalDate.of(2026, 5, 25), "Whit Monday");
        HOLIDAYS_2026.put(LocalDate.of(2026, 7, 21), "National Day");
        HOLIDAYS_2026.put(LocalDate.of(2026, 8, 15), "Assumption Day");
        HOLIDAYS_2026.put(LocalDate.of(2026, 11, 1), "All Saints' Day");
        HOLIDAYS_2026.put(LocalDate.of(2026, 11, 11), "Armistice Day");
        HOLIDAYS_2026.put(LocalDate.of(2026, 12, 25), "Christmas Day");
    }

    private final BranchRepository branches;
    private final PublicHolidayRepository holidays;
    private final FineRateRepository fineRates;
    private final LoanLimitRepository loanLimits;
    private final TitleRepository titles;
    private final CopyRepository copies;

    public SeedCatalogue load() {
        Map<String, Branch> branchByCode = seedBranches();
        seedCalendar();
        seedRates();
        seedLoanLimits();
        return seedCatalogue(branchByCode);
    }

    private Map<String, Branch> seedBranches() {
        Map<String, Branch> result = new LinkedHashMap<>();
        for (String[] b : BRANCHES) {
            Branch branch = new Branch();
            branch.setCode(b[0]);
            branch.setName(b[1]);
            result.put(b[0], branches.save(branch));
        }
        return result;
    }

    private void seedCalendar() {
        HOLIDAYS_2026.forEach((date, name) -> {
            PublicHoliday holiday = new PublicHoliday();
            holiday.setHolidayDate(date);
            holiday.setName(name);
            holidays.save(holiday);
        });
    }

    private void seedRates() {
        rate(MemberCategory.ADULT, MaterialType.BOOK, "0.25");
        rate(MemberCategory.ADULT, MaterialType.AUDIOVISUAL, "1.00");
        rate(MemberCategory.ADULT, MaterialType.MAGAZINE, "0.10");
        rate(MemberCategory.YOUTH, MaterialType.BOOK, "0.00");
        rate(MemberCategory.YOUTH, MaterialType.AUDIOVISUAL, "0.50");
        rate(MemberCategory.YOUTH, MaterialType.MAGAZINE, "0.00");
    }

    private void rate(MemberCategory category, MaterialType material, String amount) {
        FineRate rate = new FineRate();
        rate.setCategory(category);
        rate.setMaterialType(material);
        rate.setDailyRate(new BigDecimal(amount));
        rate.setValidFrom(RATES_VALID_FROM);
        fineRates.save(rate);
    }

    private void seedLoanLimits() {
        loanLimits.save(new LoanLimit(MemberCategory.ADULT, 10));
        loanLimits.save(new LoanLimit(MemberCategory.YOUTH, 5));
        loanLimits.save(new LoanLimit(MemberCategory.SENIOR, 10));
    }

    private SeedCatalogue seedCatalogue(Map<String, Branch> branchByCode) {
        List<Branch> branchList = new ArrayList<>(branchByCode.values());
        Branch central = branchByCode.get("CEN");
        AtomicInteger barcode = new AtomicInteger(100000);
        AtomicInteger isbn = new AtomicInteger(1);

        Map<MaterialType, Deque<Copy>> pool = new EnumMap<>(MaterialType.class);
        for (MaterialType type : MaterialType.values()) {
            pool.put(type, new ArrayDeque<>());
        }

        Title lighthouse = saveTitle(SeedCatalogueData.LIGHTHOUSE, isbn);
        Copy lighthouseCopy = saveCopy(lighthouse, central, CopyStatus.ON_LOAN, barcode);

        List<Title> reservedTitles = new ArrayList<>();
        List<Copy> reservedCopies = new ArrayList<>();
        for (SeedCatalogueData.TitleSeed seed : SeedCatalogueData.RESERVED_SINGLES) {
            Title title = saveTitle(seed, isbn);
            reservedTitles.add(title);
            reservedCopies.add(saveCopy(title, central, CopyStatus.ON_LOAN, barcode));
        }

        Title holdTitle = saveTitle(SeedCatalogueData.HOLD_TITLE, isbn);
        Copy holdCopy = saveCopy(holdTitle, central, CopyStatus.ON_HOLD, barcode);

        seedGeneral(SeedCatalogueData.GENERAL_BOOKS, 4, branchList, pool, barcode, isbn);
        seedGeneral(SeedCatalogueData.AUDIOVISUAL, 3, branchList, pool, barcode, isbn);
        seedGeneral(SeedCatalogueData.MAGAZINES, 2, branchList, pool, barcode, isbn);

        // One audiovisual copy with a replacement cost of 8.00 (AC-FIN-06). It never leaves the shelf in the seed.
        Copy cheapCopy = pool.get(MaterialType.AUDIOVISUAL).pollLast();
        cheapCopy.setReplacementCost(new BigDecimal("8.00"));
        copies.save(cheapCopy);

        return new SeedCatalogue(branchByCode, pool, lighthouse, lighthouseCopy, reservedTitles, reservedCopies,
                holdTitle, holdCopy);
    }

    private void seedGeneral(List<SeedCatalogueData.TitleSeed> seeds, int copiesPerTitle, List<Branch> branchList,
                             Map<MaterialType, Deque<Copy>> pool, AtomicInteger barcode, AtomicInteger isbn) {
        for (int i = 0; i < seeds.size(); i++) {
            Title title = saveTitle(seeds.get(i), isbn);
            for (int c = 0; c < copiesPerTitle; c++) {
                Branch home = branchList.get((i * 2 + c) % branchList.size());
                Copy copy = saveCopy(title, home, CopyStatus.AVAILABLE, barcode);
                pool.get(title.getMaterialType()).addLast(copy);
            }
        }
    }

    private Title saveTitle(SeedCatalogueData.TitleSeed seed, AtomicInteger isbn) {
        Title title = new Title();
        title.setTitle(seed.title());
        title.setAuthors(seed.authors());
        title.setPublisher(seed.publisher());
        title.setPublicationYear(seed.year());
        title.setMaterialType(seed.type());
        if (seed.type() == MaterialType.BOOK) {
            title.setIsbn("978100" + String.format("%07d", isbn.getAndIncrement()));
        }
        return titles.save(title);
    }

    private Copy saveCopy(Title title, Branch home, CopyStatus status, AtomicInteger barcode) {
        Copy copy = new Copy();
        copy.setBarcode("C" + barcode.getAndIncrement());
        copy.setTitle(title);
        copy.setHomeBranch(home);
        copy.setCurrentBranch(home);
        copy.setStatus(status);
        copy.setShelfMark(title.getMaterialType().name().substring(0, 3) + " " + title.getId());
        copy.setAcquisitionDate(ACQUIRED);
        return copies.save(copy);
    }
}
