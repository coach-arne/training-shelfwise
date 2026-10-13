package be.npl.shelfwise.catalogue.repository;

import be.npl.shelfwise.catalogue.entity.Branch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByCode(String code);

    List<Branch> findAllByOrderByCode();
}
