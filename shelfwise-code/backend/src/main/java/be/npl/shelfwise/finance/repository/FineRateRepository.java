package be.npl.shelfwise.finance.repository;

import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.finance.entity.FineRate;
import be.npl.shelfwise.member.entity.MemberCategory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FineRateRepository extends JpaRepository<FineRate, Long> {

    Optional<FineRate> findFirstByCategoryAndMaterialTypeAndValidFromLessThanEqualOrderByValidFromDesc(
            MemberCategory category, MaterialType materialType, LocalDate date);

    List<FineRate> findAllByOrderByValidFromDescCategoryAscMaterialTypeAsc();

    boolean existsByCategoryAndMaterialTypeAndValidFrom(
            MemberCategory category, MaterialType materialType, LocalDate validFrom);
}
