package com.example.skysport1.config;

import com.example.skysport1.entity.Account;
import com.example.skysport1.enums.RoleName;
import com.example.skysport1.repository.AccountRepository;
import com.example.skysport1.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kiểm tra toàn vẹn dữ liệu lúc khởi động app: mọi Account có role ADMIN hoặc STAFF
 * đều BẮT BUỘC phải có 1 dòng Staff tương ứng, vì rất nhiều thao tác admin/staff
 * (xác nhận đơn hàng, duyệt hoàn trả, duyệt nhập hàng...) dùng
 * StaffService.findByAccountUsername(auth.getName()) để lấy staffId — nếu thiếu
 * Staff record, các thao tác đó sẽ ném lỗi "Không tìm thấy nhân viên" khi bấm nút,
 * dù tài khoản vẫn đăng nhập và xem trang bình thường.
 *
 * Component này KHÔNG tự sửa dữ liệu (tránh side-effect ngoài ý muốn), chỉ log
 * WARNING liệt kê chính xác username nào đang thiếu, để dev/tester biết ngay
 * mà không cần bấm tay từng nút để phát hiện.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaffAccountIntegrityChecker implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final StaffRepository staffRepository;

    @Override
    public void run(String... args) {
        List<Account> staffLikeAccounts = accountRepository.findAll().stream()
                .filter(a -> a.getRole() != null)
                .filter(a -> RoleName.ADMIN.matchesId(a.getRole().getId())
                        || RoleName.STAFF.matchesId(a.getRole().getId()))
                .toList();

        List<String> missing = staffLikeAccounts.stream()
                .filter(a -> staffRepository.findByAccountId(a.getId()).isEmpty())
                .map(a -> a.getUsername() + " (role=" + a.getRole().getName()
                        + ", accountId=" + a.getId() + ")")
                .toList();

        if (missing.isEmpty()) {
            log.info("[StaffAccountIntegrityChecker] OK - tất cả {} account ADMIN/STAFF đều có Staff record tương ứng.",
                    staffLikeAccounts.size());
            return;
        }

        log.warn("==================================================================");
        log.warn("[StaffAccountIntegrityChecker] CẢNH BÁO: có {} account ADMIN/STAFF KHÔNG có Staff record đi kèm.",
                missing.size());
        log.warn("Các thao tác xác nhận/duyệt (bill, return-request, import-order) sẽ LỖI");
        log.warn("khi những account này bấm nút, dù đăng nhập và xem trang bình thường.");
        missing.forEach(m -> log.warn("  - {}", m));
        log.warn("Cách khắc phục: vào /admin/staff/create, tạo hồ sơ nhân viên và gán");
        log.warn("đúng account/role cho từng username liệt kê ở trên.");
        log.warn("==================================================================");
    }
}