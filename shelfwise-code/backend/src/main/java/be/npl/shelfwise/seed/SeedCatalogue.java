package be.npl.shelfwise.seed;

import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.catalogue.entity.Title;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/** The seeded catalogue as needed by the scenario loader. */
public record SeedCatalogue(
        Map<String, Branch> branches,
        Map<MaterialType, Deque<Copy>> pool,
        Title lighthouseTitle,
        Copy lighthouseCopy,
        List<Title> reservedTitles,
        List<Copy> reservedCopies,
        Title holdTitle,
        Copy holdCopy) {

    /** Takes a general copy out of the pool, to be used for an open loan. */
    public Copy takeCopy(MaterialType type) {
        return pool.get(type).pollFirst();
    }

    /** A general copy that stays available, used as the subject of historic (returned) loans. */
    public Copy historicCopy(MaterialType type) {
        return pool.get(type).peekLast();
    }
}
