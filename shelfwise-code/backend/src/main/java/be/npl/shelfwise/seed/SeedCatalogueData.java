package be.npl.shelfwise.seed;

import be.npl.shelfwise.catalogue.entity.MaterialType;
import java.util.List;

/** Fictional titles for the seed catalogue (Appendix B.2). */
public final class SeedCatalogueData {

    public record TitleSeed(String title, String authors, String publisher, int year, MaterialType type) {
    }

    public static final TitleSeed LIGHTHOUSE = new TitleSeed("The Lighthouse Keeper's Atlas", "Marian Holloway",
            "Brightwater Press", 2019, MaterialType.BOOK);

    /** Single copy titles that are all on loan; Leo Lane (S-12) has reservations on them. */
    public static final List<TitleSeed> RESERVED_SINGLES = List.of(
            new TitleSeed("Salt and Starlight", "Edwin Marsh", "Brightwater Press", 2021, MaterialType.BOOK),
            new TitleSeed("The Quiet Orchard", "Ines Calloway", "Northgate Books", 2018, MaterialType.BOOK),
            new TitleSeed("A Map of Small Rivers", "Tobias Reed", "Harrow & Finch", 2022, MaterialType.BOOK),
            new TitleSeed("Winter at Harrow Hall", "Philippa Lane", "Northgate Books", 2020, MaterialType.BOOK));

    /** The title that Pia Price (S-16) has on hold and Quinn Quill (S-17) is waiting for. */
    public static final TitleSeed HOLD_TITLE = new TitleSeed("The Clockmaker's Daughter", "Anselm Wright",
            "Harrow & Finch", 2017, MaterialType.BOOK);

    public static final List<TitleSeed> GENERAL_BOOKS = List.of(
            new TitleSeed("Letters from the Shore", "Dorothea Vane", "Northgate Books", 2016, MaterialType.BOOK),
            new TitleSeed("The Cartographer's Apprentice", "Julian Ashby", "Brightwater Press", 2020, MaterialType.BOOK),
            new TitleSeed("Rain on the Tin Roof", "Mabel Okoye", "Harrow & Finch", 2015, MaterialType.BOOK),
            new TitleSeed("Nine Roads to Brightwater", "Callum Frost", "Brightwater Press", 2023, MaterialType.BOOK),
            new TitleSeed("The Glassblower's Secret", "Rosalind Pike", "Northgate Books", 2014, MaterialType.BOOK),
            new TitleSeed("Northern Lights Cookbook", "Sigrid Halvorsen", "Fjord House", 2019, MaterialType.BOOK),
            new TitleSeed("A Brief History of Bridges", "Hector Dunmore", "Harrow & Finch", 2012, MaterialType.BOOK),
            new TitleSeed("Gardening for the Impatient", "Wilhelmina Cole", "Greenleaf", 2021, MaterialType.BOOK),
            new TitleSeed("The Last Ferry", "Oskar Lindqvist", "Fjord House", 2018, MaterialType.BOOK),
            new TitleSeed("Mountains of the Moon", "Priya Nandakumar", "Brightwater Press", 2022, MaterialType.BOOK),
            new TitleSeed("Understanding Tides", "Gareth Pym", "Greenleaf", 2013, MaterialType.BOOK),
            new TitleSeed("The Baker of Elmstead", "Hannah Birtwistle", "Northgate Books", 2017, MaterialType.BOOK),
            new TitleSeed("Tales of the Harbour Cats", "Lucia Ferrer", "Harrow & Finch", 2024, MaterialType.BOOK),
            new TitleSeed("Atlas of Forgotten Towns", "Marian Holloway", "Brightwater Press", 2021, MaterialType.BOOK));

    public static final List<TitleSeed> AUDIOVISUAL = List.of(
            new TitleSeed("Planet Oceans (DVD)", "Documentary Collective", "Aurora Media", 2020, MaterialType.AUDIOVISUAL),
            new TitleSeed("Concert for Strings (audio CD)", "Northwick Chamber Orchestra", "Aurora Media", 2018, MaterialType.AUDIOVISUAL),
            new TitleSeed("The Long Road Home (Blu-ray)", "Samuel Whitcombe", "Silverscreen", 2022, MaterialType.AUDIOVISUAL),
            new TitleSeed("Birdsong of Europe (audio CD)", "Nature Sounds Ltd", "Aurora Media", 2016, MaterialType.AUDIOVISUAL),
            new TitleSeed("Silent Harbour (DVD)", "Eleanor Quist", "Silverscreen", 2019, MaterialType.AUDIOVISUAL),
            new TitleSeed("Kitchen Stories (DVD)", "Tomas Berg", "Silverscreen", 2015, MaterialType.AUDIOVISUAL));

    public static final List<TitleSeed> MAGAZINES = List.of(
            new TitleSeed("Nature Monthly - March 2025", "Various", "Greenleaf Periodicals", 2025, MaterialType.MAGAZINE),
            new TitleSeed("Cooking Today - June 2025", "Various", "Hearth & Home", 2025, MaterialType.MAGAZINE),
            new TitleSeed("Science Weekly - Issue 212", "Various", "Meridian Publishing", 2025, MaterialType.MAGAZINE),
            new TitleSeed("Travel & Leisure - Autumn 2025", "Various", "Meridian Publishing", 2025, MaterialType.MAGAZINE));

    private SeedCatalogueData() {
    }
}
