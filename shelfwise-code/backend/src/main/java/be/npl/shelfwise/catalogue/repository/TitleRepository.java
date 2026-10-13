package be.npl.shelfwise.catalogue.repository;

import be.npl.shelfwise.catalogue.entity.Title;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TitleRepository extends JpaRepository<Title, Long> {

    /** Keyword search over title, authors, publisher and ISBN. {@code pattern} is a lower-case like pattern. */
    @Query("""
            select t from Title t
            where lower(t.title) like :pattern
               or lower(t.authors) like :pattern
               or lower(coalesce(t.publisher, '')) like :pattern
               or lower(coalesce(t.isbn, '')) like :pattern
            order by t.title
            """)
    List<Title> searchKeyword(@Param("pattern") String pattern, Pageable pageable);

    @Query("select t from Title t where lower(t.title) like :pattern order by t.title")
    List<Title> searchByTitle(@Param("pattern") String pattern, Pageable pageable);

    @Query("select t from Title t where lower(t.authors) like :pattern order by t.title")
    List<Title> searchByAuthor(@Param("pattern") String pattern, Pageable pageable);

    @Query("select t from Title t where replace(coalesce(t.isbn, ''), '-', '') like :pattern order by t.title")
    List<Title> searchByIsbn(@Param("pattern") String pattern, Pageable pageable);
}
