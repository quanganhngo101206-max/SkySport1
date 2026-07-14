package com.example.skysport1.service;

import com.example.skysport1.entity.Customer;
import com.example.skysport1.entity.Notification;
import com.example.skysport1.entity.Staff;
import com.example.skysport1.repository.NotificationRepository;
import com.example.skysport1.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final StaffRepository staffRepository;

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

    /**
     * Gửi thông báo cho TẤT CẢ admin đang hoạt động — dùng cho các sự kiện
     * cần admin biết ngay: đơn hàng mới, phiếu nhập mới, khách hàng mới đăng ký.
     * Lỗi khi gửi (nếu có) chỉ log, không làm rollback nghiệp vụ chính.
     */
    @Transactional
    public void notifyAllAdmins(String title, String content, String type, String referenceId) {
        try {
            List<Staff> admins = staffRepository.findAllActiveAdmins();
            for (Staff admin : admins) {
                sendToStaff(admin.getId(), title, content, type, referenceId);
            }
        } catch (Exception e) {
            log.error("Không thể gửi thông báo cho admin (type={}, ref={}): {}", type, referenceId, e.getMessage());
        }
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