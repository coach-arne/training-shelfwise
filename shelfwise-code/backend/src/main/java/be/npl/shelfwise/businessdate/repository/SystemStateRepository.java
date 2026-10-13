package be.npl.shelfwise.businessdate.repository;

import be.npl.shelfwise.businessdate.entity.SystemState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemStateRepository extends JpaRepository<SystemState, Integer> {
}
