package be.npl.shelfwise.catalogue.repository;

import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.CopyStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CopyRepository extends JpaRepository<Copy, Long> {

    Optional<Copy> findByBarcode(String barcode);

    List<Copy> findByTitleIdOrderByBarcode(Long titleId);

    List<Copy> findByTitleIdIn(Collection<Long> titleIds);

    boolean existsByTitleIdAndCurrentBranchIdAndStatus(Long titleId, Long branchId, CopyStatus status);

    long countByTitleIdAndStatusNotIn(Long titleId, Collection<CopyStatus> statuses);

    long countByTitleId(Long titleId);

    /** Rows of (titleId, branchId, count) for AVAILABLE copies of the given titles. */
    @Query("""
            select c.title.id, c.currentBranch.id, count(c)
            from Copy c
            where c.title.id in :titleIds and c.status = be.npl.shelfwise.catalogue.entity.CopyStatus.AVAILABLE
            group by c.title.id, c.currentBranch.id
            """)
    List<Object[]> availabilityRows(@Param("titleIds") Collection<Long> titleIds);

    boolean existsByBarcode(String barcode);
}
