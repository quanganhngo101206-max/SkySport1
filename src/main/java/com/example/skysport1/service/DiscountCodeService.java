package com.example.skysport1.service;

import com.example.skysport1.entity.CustomerDiscount;
import com.example.skysport1.entity.DiscountCode;
import com.example.skysport1.exception.AppException;
import com.example.skysport1.exception.DuplicateException;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.CustomerDiscountRepository;
import com.example.skysport1.repository.DiscountCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountCodeService {

    private final DiscountCodeRepository discountCodeRepository;
    private final CustomerDiscountRepository customerDiscountRepository;

    public List<DiscountCode> findAll() {
        return discountCodeRepository.findAll();
    }

    public DiscountCode findById(Integer id) {
        return discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("mã giảm giá", String.valueOf(id)));
    }

    public DiscountCode findByCode(String code) {
        return discountCodeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("mã giảm giá", code));
    }

    @Transactional
    public DiscountCode save(DiscountCode discountCode) {
        if (discountCodeRepository.existsByCode(discountCode.getCode())) {
            throw new DuplicateException("Mã giảm giá đã tồn tại: " + discountCode.getCode());
        }
        discountCode.setUsedCount(0);
        discountCode.setStatus(1);
        discountCode.setDeleteFlag(false);
        return discountCodeRepository.save(discountCode);
    }

    @Transactional
    public void delete(Integer id) {
        DiscountCode dc = findById(id);
        dc.setDeleteFlag(true);
        discountCodeRepository.save(dc);
    }

    /**
     * Validate và tính tiền giảm cho 1 đơn hàng.
     * Dùng để hiển thị preview số tiền được giảm (ví dụ ở trang checkout trước
     * khi khách bấm đặt hàng) — KHÔNG khoá dòng, vì chỉ đọc để hiển thị, chưa
     * thực sự tạo đơn. Luồng tạo đơn thật sự phải dùng {@link #lockAndValidate}.
     *
     * @return Số tiền được giảm (đã áp dụng max_discount_value)
     */
    public BigDecimal validate(String code, String customerId, BigDecimal orderTotal) {
        DiscountCode dc = findByCode(code);
        validateRules(dc, customerId, orderTotal);
        return calculateDiscountAmount(dc, orderTotal);
    }

    /**
     * Khoá dòng discount_code (SELECT ... FOR UPDATE) rồi validate, dùng trong
     * luồng tạo đơn hàng thực tế (BillServiceImpl.doCreateBill). Giữ khoá xuyên
     * suốt transaction tới lúc tăng usedCount, để 2 đơn cùng dùng 1 voucher sắp
     * hết lượt gần như đồng thời không thể cùng pass bước kiểm tra lượt dùng
     * trước khi bên nào tăng usedCount xong (tránh oversell voucher) — đối xứng
     * với cách ProductDetailRepository.findByIdForUpdate() bảo vệ tồn kho.
     *
     * @return DiscountCode entity đã bị khoá, dùng lại để tính tiền giảm và
     *         tăng usedCount mà không phải query lại (đọc lại vẫn cùng dòng đã khoá).
     */
    @Transactional
    public DiscountCode lockAndValidate(String code, String customerId, BigDecimal orderTotal) {
        DiscountCode dc = discountCodeRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new ResourceNotFoundException("mã giảm giá", code));
        validateRules(dc, customerId, orderTotal);
        return dc;
    }

    private void validateRules(DiscountCode dc, String customerId, BigDecimal orderTotal) {
        // Kiểm tra trạng thái
        if (dc.getStatus() != 1 || Boolean.TRUE.equals(dc.getDeleteFlag())) {
            throw new AppException("Mã giảm giá không còn hiệu lực");
        }
        // Kiểm tra thời hạn
        LocalDateTime now = LocalDateTime.now();
        if (dc.getStartDate() != null && now.isBefore(dc.getStartDate())) {
            throw new AppException("Mã giảm giá chưa có hiệu lực");
        }
        if (dc.getEndDate() != null && now.isAfter(dc.getEndDate())) {
            throw new AppException("Mã giảm giá đã hết hạn");
        }
        // Kiểm tra lượt dùng
        if (dc.getQuantity() != null && dc.getUsedCount() >= dc.getQuantity()) {
            throw new AppException("Mã giảm giá đã hết lượt sử dụng");
        }
        // Kiểm tra giá trị đơn tối thiểu
        if (dc.getMinOrderValue() != null && orderTotal.compareTo(dc.getMinOrderValue()) < 0) {
            throw new AppException("Đơn hàng chưa đạt giá trị tối thiểu "
                    + dc.getMinOrderValue() + " để dùng mã này");
        }
        // Kiểm tra customer đã dùng chưa
        if (customerId != null
                && customerDiscountRepository.existsByCustomerIdAndDiscountCodeId(customerId, dc.getId())) {
            throw new AppException("Bạn đã sử dụng mã giảm giá này rồi");
        }
    }

    public BigDecimal calculateDiscountAmount(DiscountCode dc, BigDecimal orderTotal) {
        BigDecimal discount;
        if (dc.getDiscountType() == 1) {
            // Cố định
            discount = dc.getDiscountValue();
        } else {
            // Phần trăm
            discount = orderTotal.multiply(dc.getDiscountValue())
                    .divide(BigDecimal.valueOf(100));
        }
        // Áp dụng giới hạn giảm tối đa
        if (dc.getMaxDiscountValue() != null && discount.compareTo(dc.getMaxDiscountValue()) > 0) {
            discount = dc.getMaxDiscountValue();
        }
        return discount;
    }

    /**
     * Tăng used_count sau khi bill được tạo thành công.
     */
    @Transactional
    public void incrementUsedCount(Integer discountCodeId) {
        incrementUsedCount(findById(discountCodeId));
    }

    /**
     * Tăng used_count trên entity đã có sẵn (ví dụ entity vừa được khoá bởi
     * {@link #lockAndValidate}) — tránh phải query lại DB một lần nữa.
     */
    @Transactional
    public void incrementUsedCount(DiscountCode dc) {
        dc.setUsedCount(dc.getUsedCount() + 1);
        discountCodeRepository.save(dc);
    }

    /**
     * Ghi nhận khách hàng đã dùng voucher.
     */
    @Transactional
    public void recordUsage(String customerId, Integer discountCodeId, String billId) {
        recordUsage(customerId, DiscountCode.builder().id(discountCodeId).build(), billId);
    }

    /**
     * Ghi nhận khách hàng đã dùng voucher, dùng entity đã có sẵn (ví dụ entity
     * vừa được khoá bởi {@link #lockAndValidate}) để tránh query lại DB.
     */
    @Transactional
    public void recordUsage(String customerId, DiscountCode discountCode, String billId) {
        // Tạo CustomerDiscount entity trực tiếp để tránh circular dependency
        CustomerDiscount usage = CustomerDiscount.builder()
                .customer(com.example.skysport1.entity.Customer.builder().id(customerId).build())
                .discountCode(discountCode)
                .bill(com.example.skysport1.entity.Bill.builder().id(billId).build())
                .build();
        customerDiscountRepository.save(usage);
        incrementUsedCount(discountCode);
    }
}