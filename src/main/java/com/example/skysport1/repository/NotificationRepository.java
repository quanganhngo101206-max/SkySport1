package com.example.skysport1.repository;

import com.example.skysport1.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByCustomerIdAndIsReadOrderByCreateDateDesc(String customerId, Boolean isRead);
    List<Notification> findByStaffIdAndIsReadOrderByCreateDateDesc(String staffId, Boolean isRead);
    List<Notification> findByCustomerIdOrderByCreateDateDesc(String customerId);
    long countByCustomerIdAndIsRead(String customerId, Boolean isRead);
}
