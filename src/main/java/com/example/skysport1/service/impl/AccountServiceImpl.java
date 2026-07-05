package com.example.skysport1.service.impl;

import com.example.skysport1.dto.request.RegisterRequest;
import com.example.skysport1.entity.Account;
import com.example.skysport1.entity.Customer;
import com.example.skysport1.entity.Role;
import com.example.skysport1.enums.RoleName;
import com.example.skysport1.exception.AppException;
import com.example.skysport1.exception.DuplicateException;
import com.example.skysport1.repository.AccountRepository;
import com.example.skysport1.repository.CustomerRepository;
import com.example.skysport1.repository.RoleRepository;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;

    @Override
    @Transactional
    public Account register(RegisterRequest request) {
        // Validate trùng username/email
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateException("Tên đăng nhập đã tồn tại");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && accountRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateException("Email đã được sử dụng");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException("Mật khẩu xác nhận không khớp");
        }
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateException("Số điện thoại đã được đăng ký");
        }

        // Lấy role CUSTOMER
        Role role = roleRepository.findById(RoleName.CUSTOMER.getId())
                .orElseThrow(() -> new AppException("Không tìm thấy role CUSTOMER"));

        // Tạo Account
        Account account = Account.builder()
                .id(idGenerator.generateAccountId())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .status(1)
                .isNonLocked(true)
                .role(role)
                .build();
        account = accountRepository.save(account);

        // Tạo Customer gắn với Account
        Customer customer = Customer.builder()
                .id(idGenerator.generateCustomerId())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .account(account)
                .status(1)
                .deleteFlag(false)
                .build();
        customerRepository.save(customer);

        log.info("Đăng ký thành công: {} ({})", account.getUsername(), account.getId());
        return account;
    }

    @Override
    public Account findByUsername(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Không tìm thấy tài khoản: " + username));
    }

    @Override
    public boolean existsByUsername(String username) {
        return accountRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return accountRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void updatePassword(String username, String encodedPassword) {
        Account account = findByUsername(username);
        account.setPassword(encodedPassword);
        accountRepository.save(account);
        log.info("Password updated for account: {}", username);
    }

    @Override
    @Transactional
    public Account findByUsernameWithRole(String username) {
        return accountRepository.findByUsernameWithRole(username)
                .orElseThrow(() -> new AppException("Không tìm thấy tài khoản: " + username));
    }
}