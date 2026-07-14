package com.example.skysport1.service;

import com.example.skysport1.entity.BillDetail;
import com.example.skysport1.entity.CartDetail;
import com.example.skysport1.entity.CustomerDiscount;
import com.example.skysport1.entity.DiscountCode;
import com.example.skysport1.exception.AppException;
import com.example.skysport1.exception.DuplicateException;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.BillRepository;
import com.example.skysport1.repository.CustomerDiscountRepository;
import com.example.skysport1.repository.CustomerRepository;
import com.example.skysport1.repository.DiscountCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountCodeService {

    private static final Integer ORDER_STATUS_COMPLETED = 7; // OrderStatus.COMPLETED

    private final DiscountCodeRepository discountCodeRepository;
    private final CustomerDiscountRepository customerDiscountRepository;
    private final CustomerRepository customerRepository;
    private final BillRepository billRepository;

    /**
     * Dòng hàng rút gọn (productId + thành tiền) dùng để tính discount theo
     * đúng danh sách sản phẩm được áp dụng, không phụ thuộc CartDetail hay
     * BillDetail cụ thể — 2 luồng preview (giỏ hàng) và tạo đơn thật (bill)
     * dùng chung 1 chỗ tính tiền, tránh lệch logic giữa preview và lúc đặt hàng.
     */
    public record LineItem(String productId, BigDecimal lineTotal) {}

    public static List<LineItem> fromCartDetails(List<CartDetail> items) {
        List<LineItem> result = new ArrayList<>();
        for (CartDetail cd : items) {
            if (cd.getProductDetail() == null || cd.getProductDetail().getPrice() == null) continue;
            String productId = cd.getProductDetail().getProduct() != null
                    ? cd.getProductDetail().getProduct().getId() : null;
            BigDecimal lineTotal = cd.getProductDetail().getPrice().multiply(BigDecimal.valueOf(cd.getQuantity()));
            result.add(new LineItem(productId, lineTotal));
        }
        return result;
    }

    public static List<LineItem> fromBillDetails(List<BillDetail> items) {
        List<LineItem> result = new ArrayList<>();
        for (BillDetail bd : items) {
            String productId = bd.getProductDetail() != null && bd.getProductDetail().getProduct() != null
                    ? bd.getProductDetail().getProduct().getId() : null;
            result.add(new LineItem(productId, bd.getTotalAmount()));
        }
        return result;
    }

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
     * Sửa mã giảm giá đã tồn tại. Cố ý KHÔNG cho sửa "code" (mã) và không
     * đụng tới usedCount/deleteFlag — những giá trị đó gắn với lịch sử sử
     * dụng thực tế, sửa sẽ làm sai lệch dữ liệu đã phát sinh. Admin chỉ sửa
     * các thông tin cấu hình: tên, loại/giá trị giảm, điều kiện, thời hạn,
     * số lượng, trạng thái bật/tắt.
     */
    @Transactional
    public DiscountCode update(Integer id, DiscountCode data) {
        DiscountCode dc = findById(id);
        if (data.getQuantity() != null && data.getQuantity() < dc.getUsedCount()) {
            throw new AppException("Số lượng không thể thấp hơn số lượt đã dùng (" + dc.getUsedCount() + ")");
        }
        dc.setName(data.getName());
        dc.setDiscountType(data.getDiscountType());
        dc.setDiscountValue(data.getDiscountValue());
        dc.setMinOrderValue(data.getMinOrderValue());
        dc.setMaxDiscountValue(data.getMaxDiscountValue());
        dc.setQuantity(data.getQuantity());
        dc.setStartDate(data.getStartDate());
        dc.setEndDate(data.getEndDate());
        if (data.getApplicableCustomerGroup() != null) {
            dc.setApplicableCustomerGroup(data.getApplicableCustomerGroup());
        }
        // maxUsagePerCustomer: cho phép set về null (không giới hạn) nên không
        // thể chỉ check "!= null" như các field khác — luôn gán theo data gửi lên,
        // controller sẽ gửi null rõ ràng khi admin chọn "không giới hạn".
        dc.setMaxUsagePerCustomer(data.getMaxUsagePerCustomer());
        if (data.getApplicableProducts() != null) {
            dc.setApplicableProducts(data.getApplicableProducts());
        }
        if (data.getStatus() != null) {
            dc.setStatus(data.getStatus());
        }
        if (data.getUpdatedBy() != null) {
            dc.setUpdatedBy(data.getUpdatedBy());
        }
        return discountCodeRepository.save(dc);
    }

    /**
     * Validate và tính tiền giảm cho 1 đơn hàng.
     * Dùng để hiển thị preview số tiền được giảm (ví dụ ở trang checkout trước
     * khi khách bấm đặt hàng) — KHÔNG khoá dòng, vì chỉ đọc để hiển thị, chưa
     * thực sự tạo đơn. Luồng tạo đơn thật sự phải dùng {@link #lockAndValidate}.
     *
     * @return Số tiền được giảm (đã áp dụng max_discount_value, chỉ tính trên
     *         các sản phẩm nằm trong danh sách áp dụng của mã, nếu có cấu hình)
     */
    public BigDecimal validate(String code, String customerId, List<LineItem> items) {
        DiscountCode dc = findByCode(code);
        BigDecimal orderTotal = items.stream().map(LineItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        validateRules(dc, customerId, orderTotal);
        return calculateDiscountAmount(dc, items);
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
    public DiscountCode lockAndValidate(String code, String customerId, List<LineItem> items) {
        DiscountCode dc = discountCodeRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new ResourceNotFoundException("mã giảm giá", code));
        BigDecimal orderTotal = items.stream().map(LineItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
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
        // Kiểm tra số lần khách đã dùng mã này so với giới hạn cấu hình được
        // (maxUsagePerCustomer null = không giới hạn số lần/khách, chỉ còn bị
        // chặn bởi tổng lượt dùng chung của mã ở check quantity phía trên)
        if (customerId != null && dc.getMaxUsagePerCustomer() != null) {
            long usedByCustomer = customerDiscountRepository.countByCustomerIdAndDiscountCodeId(customerId, dc.getId());
            if (usedByCustomer >= dc.getMaxUsagePerCustomer()) {
                throw new AppException("Bạn đã dùng mã giảm giá này đủ số lần cho phép ("
                        + dc.getMaxUsagePerCustomer() + " lần)");
            }
        }
        // Kiểm tra điều kiện nhóm khách hàng (0: tất cả, 1: khách mới, 2: VIP)
        Integer group = dc.getApplicableCustomerGroup();
        if (group != null && group != 0) {
            if (customerId == null) {
                // Guest không xác định được nhóm -> không cho dùng mã có điều kiện nhóm
                throw new AppException("Mã giảm giá này chỉ áp dụng cho một số nhóm khách hàng, vui lòng đăng nhập");
            }
            if (group == 1) {
                boolean hasCompletedOrder = billRepository.existsByCustomerIdAndStatus(customerId, ORDER_STATUS_COMPLETED);
                if (hasCompletedOrder) {
                    throw new AppException("Mã giảm giá này chỉ áp dụng cho khách hàng mới");
                }
            } else if (group == 2) {
                var customer = customerRepository.findById(customerId).orElse(null);
                if (customer == null || !Boolean.TRUE.equals(customer.getIsVip())) {
                    throw new AppException("Mã giảm giá này chỉ áp dụng cho khách hàng VIP");
                }
            }
        }
    }

    /**
     * Tính tiền giảm — nếu mã có cấu hình danh sách sản phẩm áp dụng
     * (applicableProducts không rỗng), chỉ tính trên tổng giá trị các dòng
     * hàng thuộc danh sách đó; sản phẩm khác trong cùng đơn giữ nguyên giá.
     * Nếu danh sách rỗng, áp dụng như cũ trên toàn bộ đơn.
     */
    public BigDecimal calculateDiscountAmount(DiscountCode dc, List<LineItem> items) {
        BigDecimal qualifyingSubtotal;
        if (dc.getApplicableProducts() != null && !dc.getApplicableProducts().isEmpty()) {
            Set<String> applicableIds = dc.getApplicableProducts().stream()
                    .map(p -> p.getId())
                    .collect(Collectors.toSet());
            qualifyingSubtotal = items.stream()
                    .filter(li -> li.productId() != null && applicableIds.contains(li.productId()))
                    .map(LineItem::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            qualifyingSubtotal = items.stream().map(LineItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        if (qualifyingSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (dc.getDiscountType() == 1) {
            // Cố định — nhưng không vượt quá giá trị các sản phẩm áp dụng được
            discount = dc.getDiscountValue().min(qualifyingSubtotal);
        } else {
            // Phần trăm, tính trên phần giá trị áp dụng được
            discount = qualifyingSubtotal.multiply(dc.getDiscountValue())
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