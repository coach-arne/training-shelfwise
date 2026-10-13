package be.npl.shelfwise.notification.repository;

import be.npl.shelfwise.notification.entity.Notification;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByIdDesc(Pageable pageable);
}
