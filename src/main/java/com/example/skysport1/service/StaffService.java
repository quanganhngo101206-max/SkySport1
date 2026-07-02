package com.example.skysport1.service;

import com.example.skysport1.dto.request.StaffCreateRequest;
import com.example.skysport1.dto.request.StaffUpdateRequest;
import com.example.skysport1.entity.Account;
import com.example.skysport1.entity.Role;
import com.example.skysport1.entity.Staff;
import com.example.skysport1.exception.DuplicateException;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.AccountRepository;
import com.example.skysport1.repository.RoleRepository;
import com.example.skysport1.repository.StaffRepository;
import com.example.skysport1.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;

    public List<Staff> findAll() {
        return staffRepository.findByDeleteFlagFalseOrderByFullNameAsc();
    }

    public Staff findById(String id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("nhân viên", id));
    }

    public Staff findByAccountId(String accountId) {
        return staffRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("nhân viên", accountId));
    }

    public List<Staff> search(String keyword) {
        return staffRepository.searchByName(keyword);
    }

    /**
     * Tạo nhân viên mới kèm tài khoản đăng nhập. Toàn bộ logic dựng entity
     * nằm trong service — controller chỉ truyền DTO đã validate.
     */
    @Transactional
    public Staff create(StaffCreateRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateException("Tên đăng nhập '" + request.getUsername() + "' đã tồn tại!");
        }
        if (request.getPhone() != null && staffRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateException("Số điện thoại đã được sử dụng");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("role", request.getRoleId()));

        Account account = Account.builder()
                .id(idGenerator.generateAccountId())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(1)
                .isNonLocked(true)
                .role(role)
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();
        account = accountRepository.save(account);

        Staff staff = Staff.builder()
                .id(idGenerator.generateStaffId())
                .account(account)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .gender(request.getGender())
                .dob(request.getDob())
                .address(request.getAddress())
                .hireDate(LocalDate.now())
                .status(1)
                .deleteFlag(false)
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        return staffRepository.save(staff);
    }

    /**
     * Cập nhật thông tin nhân viên. Nếu request.password có giá trị thì đổi
     * luôn mật khẩu tài khoản gắn với nhân viên đó.
     */
    @Transactional
    public Staff update(String id, StaffUpdateRequest request) {
        Staff staff = findById(id);

        if (request.getPhone() != null
                && !request.getPhone().equals(staff.getPhone())
                && staffRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateException("Số điện thoại đã được sử dụng");
        }

        staff.setFullName(request.getFullName());
        staff.setPhone(request.getPhone());
        staff.setEmail(request.getEmail());
        staff.setGender(request.getGender());
        staff.setDob(request.getDob());
        staff.setAddress(request.getAddress());
        if (request.getStatus() != null) {
            staff.setStatus(request.getStatus());
        }
        staff.setUpdateDate(LocalDateTime.now());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            Account account = staff.getAccount();
            account.setPassword(passwordEncoder.encode(request.getPassword()));
            account.setUpdateDate(LocalDateTime.now());
            accountRepository.save(account);
        }

        return staffRepository.save(staff);
    }

    @Transactional
    public void delete(String id) {
        Staff staff = findById(id);
        staff.setDeleteFlag(true);
        staff.setStatus(0);
        staff.setUpdateDate(LocalDateTime.now());
        staffRepository.save(staff);
    }

    public Staff findByAccountUsername(String username) {
        return staffRepository.findByAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tài khoản '" + username + "' chưa được liên kết với hồ sơ nhân viên (Staff). "
                                + "Vào Quản lý nhân viên (/admin/staff) để tạo hồ sơ và gán cho tài khoản này trước khi thực hiện thao tác."));
    }

    /**
     * Lấy DTO phục vụ màn edit nhân viên (HƯỚNG B: template bind staffRequest).
     */
    @Transactional(readOnly = true)
    public StaffUpdateRequest findUpdateRequestById(String id) {
        Staff staff = findById(id);

        StaffUpdateRequest dto = new StaffUpdateRequest();
        dto.setId(staff.getId());
        dto.setFullName(staff.getFullName());
        dto.setPhone(staff.getPhone());
        dto.setEmail(staff.getEmail());
        dto.setGender(staff.getGender());
        dto.setDob(staff.getDob());
        dto.setAddress(staff.getAddress());
        dto.setStatus(staff.getStatus());

        // DTO có field username để hiển thị (không dùng để chỉnh sửa)
        if (staff.getAccount() != null) {
            dto.setUsername(staff.getAccount().getUsername());
        }

        // password để trống -> không đổi mật khẩu
        dto.setPassword(null);

        return dto;
    }
}