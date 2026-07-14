package com.example.skysport1.service;

import com.example.skysport1.dto.request.CustomerUpdateRequest;
import com.example.skysport1.entity.Customer;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer findById(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("khách hàng", id));
    }

    public Customer findByAccountId(String accountId) {
        return customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("khách hàng", accountId));
    }

    public Customer findByPhone(String phone) {
        return customerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("khách hàng", phone));
    }

    /**
     * Admin CHỈ được phép khoá/mở khoá tài khoản khách hàng (status) tại đây.
     * Họ tên/SĐT/email/giới tính/ngày sinh là dữ liệu định danh của khách,
     * cố ý KHÔNG cho admin sửa trực tiếp để tránh việc sửa nhầm ảnh hưởng
     * tới khách hàng — khách tự sửa những trường đó qua trang hồ sơ của họ
     * (CustomerProfileController). fullName/phone/email/... trong
     * CustomerUpdateRequest chỉ được form gửi lên ở dạng readonly để hiển
     * thị, service này bỏ qua, không ghi đè.
     */
    @Transactional
    public Customer update(String id, CustomerUpdateRequest request) {
        Customer customer = findById(id);
        if (request.getStatus() != null) {
            customer.setStatus(request.getStatus());
        }
        return customerRepository.save(customer);
    }

    @Transactional
    public void delete(String id) {
        Customer customer = findById(id);
        customer.setDeleteFlag(true);
        customer.setStatus(0);
        customerRepository.save(customer);
    }

    public List<Customer> findAllActive() {
        return customerRepository.findByDeleteFlagFalseAndStatus(1);
    }
}