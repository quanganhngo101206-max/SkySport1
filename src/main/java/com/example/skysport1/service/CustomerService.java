package com.example.skysport1.service;

import com.example.skysport1.dto.request.CustomerUpdateRequest;
import com.example.skysport1.entity.Customer;
import com.example.skysport1.exception.DuplicateException;
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

    @Transactional
    public Customer update(String id, CustomerUpdateRequest request) {
        Customer customer = findById(id);
        // Kiểm tra phone trùng nếu đổi
        if (request.getPhone() != null
                && !request.getPhone().equals(customer.getPhone())
                && customerRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateException("Số điện thoại đã được sử dụng");
        }
        customer.setFullName(request.getFullName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setGender(request.getGender());
        customer.setDob(request.getDob());
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