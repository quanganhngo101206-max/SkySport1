package com.example.skysport1.service;

import com.example.skysport1.entity.Customer;
import com.example.skysport1.entity.Notification;
import com.example.skysport1.entity.Staff;
import com.example.skysport1.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> findByCustomer(String customerId) {
        return notificationRepository.findByCustomerIdOrderByCreateDateDesc(customerId);
    }

    public List<Notification> findUnreadByCustomer(String customerId) {
        return notificationRepository.findByCustomerIdAndIsReadOrderByCreateDateDesc(customerId, false);
    }

    public List<Notification> findUnreadByStaff(String staffId) {
        return notificationRepository.findByStaffIdAndIsReadOrderByCreateDateDesc(staffId, false);
    }

    public long countUnread(String customerId) {
        return notificationRepository.countByCustomerIdAndIsRead(customerId, false);
    }

    @Transactional
    public void sendToCustomer(String customerId, String title, String content,
                               String type, String referenceId) {
        Notification notification = Notification.builder()
                .customer(Customer.builder().id(customerId).build())
                .title(title)
                .content(content)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional
    public void sendToStaff(String staffId, String title, String content,
                            String type, String referenceId) {
        Notification notification = Notification.builder()
                .staff(Staff.builder().id(staffId).build())
                .title(title)
                .content(content)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAsRead(Integer notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(String customerId) {
        List<Notification> unread = findUnreadByCustomer(customerId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }
}
