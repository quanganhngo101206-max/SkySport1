package com.example.skysport1.service.impl;

import com.example.skysport1.entity.*;
import com.example.skysport1.enums.InventoryActionType;
import com.example.skysport1.enums.NotificationType;
import com.example.skysport1.enums.OrderStatus;
import com.example.skysport1.enums.PaymentStatus;
import com.example.skysport1.exception.AppException;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.*;
import com.example.skysport1.service.BillService;
import com.example.skysport1.service.DiscountCodeService;
import com.example.skysport1.service.NotificationService;
import com.example.skysport1.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;
    private final BillDetailRepository billDetailRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ProductDetailRepository productDetailRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountCodeService discountCodeService;
    private final NotificationService notificationService;
    private final IdGenerator idGenerator;

    // ── Enum quản lý chuyển trạng thái ────────────────────────────────────
    private enum AllowedTransition {
        PENDING_TO_CONFIRMED(OrderStatus.PENDING.getValue(), OrderStatus.CONFIRMED.getValue()),
        CONFIRMED_TO_SHIPPING(OrderStatus.CONFIRMED.getValue(), OrderStatus.SHIPPING.getValue()),
        SHIPPING_TO_DELIVERED(OrderStatus.SHIPPING.getValue(), OrderStatus.DELIVERED.getValue()),
        DELIVERED_TO_COMPLETED(OrderStatus.DELIVERED.getValue(), OrderStatus.COMPLETED.getValue()),
        PENDING_TO_CANCELLED(OrderStatus.PENDING.getValue(), OrderStatus.CANCELLED.getValue()),
        CONFIRMED_TO_CANCELLED(OrderStatus.CONFIRMED.getValue(), OrderStatus.CANCELLED.getValue()),
        DELIVERED_TO_RETURNING(OrderStatus.DELIVERED.getValue(), OrderStatus.RETURNING.getValue()),
        COMPLETED_TO_RETURNING(OrderStatus.COMPLETED.getValue(), OrderStatus.RETURNING.getValue());

        private final int from;
        private final int to;

        AllowedTransition(int from, int to) {
            this.from = from;
            this.to = to;
        }

        public static boolean isAllowed(int from, int to) {
            for (AllowedTransition t : values()) {
                if (t.from == from && t.to == to) {
                    return true;
                }
            }
            return false;
        }
    }

    // ── Query ──────────────────────────────────────────────────────────────

    @Override
    public Bill findById(String id) {
        return billRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("đơn hàng", id));
    }

    @Override
    public List<Bill> findByCustomerId(String customerId) {
        return billRepository.findByCustomerIdOrderByCreateDateDesc(customerId);
    }

    @Override
    public List<Bill> findByStatus(Integer status) {
        return billRepository.findByStatus(status);
    }

    @Override
    public Page<Bill> findByStatusPaged(Integer status, Pageable pageable) {
        return billRepository.findByStatusWithCustomer(status, pageable);
    }

    @Override
    public List<Bill> findByGuestEmail(String guestEmail) {
        return billRepository.findByGuestEmail(guestEmail);
    }

    @Override
    public List<Bill> findAll() {
        return billRepository.findAll();
    }

    @Override
    public Page<Bill> findAllPaged(Pageable pageable) {
        return billRepository.findAllWithCustomer(pageable);
    }

    // ── Tạo đơn ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Bill createOnlineBill(String customerId,
                                 String shippingAddress,
                                 String receiverName,
                                 String receiverPhone,
                                 String paymentId,
                                 String discountCode,
                                 List<BillDetail> items) {
        Bill bill = Bill.builder()
                .id(idGenerator.generateBillId())
                .customer(Customer.builder().id(customerId).build())
                .shippingAddress(shippingAddress)
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .payment(Payment.builder().id(paymentId).build())
                .invoiceType(1)
                .status(OrderStatus.PENDING.getValue())
                .paymentStatus(PaymentStatus.UNPAID.getValue())
                .build();

        return doCreateBill(bill, items, discountCode, customerId);
    }

    @Override
    @Transactional
    public Bill createGuestBill(String guestEmail,
                                String shippingAddress,
                                String receiverName,
                                String receiverPhone,
                                String paymentId,
                                String discountCode,
                                List<BillDetail> items) {
        Bill bill = Bill.builder()
                .id(idGenerator.generateBillId())
                .guestEmail(guestEmail)
                .shippingAddress(shippingAddress)
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .payment(Payment.builder().id(paymentId).build())
                .invoiceType(1)
                .status(OrderStatus.PENDING.getValue())
                .paymentStatus(PaymentStatus.UNPAID.getValue())
                .build();

        return doCreateBill(bill, items, discountCode, null);
    }

    @Override
    @Transactional
    public Bill createCounterBill(String customerId,
                                  String paymentId,
                                  String staffId,
                                  List<BillDetail> items) {
        Bill bill = Bill.builder()
                .id(idGenerator.generateBillId())
                .customer(customerId != null ? Customer.builder().id(customerId).build() : null)
                .payment(Payment.builder().id(paymentId).build())
                .invoiceType(2)
                .status(OrderStatus.COMPLETED.getValue())
                .paymentStatus(PaymentStatus.PAID.getValue())
                .createdBy(staffId)
                .build();

        bill = doCreateBill(bill, items, null, customerId);

        logHistory(bill, null, OrderStatus.COMPLETED.getValue(),
                "Bán tại quầy - hoàn thành ngay", staffId);

        return bill;
    }

    private Bill doCreateBill(Bill bill, List<BillDetail> items,
                              String discountCode, String customerId) {
        if (items == null || items.isEmpty()) {
            throw new AppException("Đơn hàng phải có ít nhất 1 sản phẩm");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<BillDetail> savedDetails = new ArrayList<>();

        for (BillDetail item : items) {
            ProductDetail pd = productDetailRepository
                    .findById(item.getProductDetail().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "sản phẩm", String.valueOf(item.getProductDetail().getId())));

            if (pd.getStatus() != 1 || Boolean.TRUE.equals(pd.getDeleteFlag())) {
                throw new AppException("Sản phẩm '" + pd.getSku() + "' không còn bán");
            }
            if (pd.getQuantity() < item.getQuantity()) {
                throw new AppException("Sản phẩm '" + pd.getSku()
                        + "' không đủ tồn kho (còn " + pd.getQuantity() + ")");
            }

            BigDecimal lineTotal = pd.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            BillDetail detail = BillDetail.builder()
                    .productDetail(pd)
                    .productNameSnapshot(pd.getProduct().getName())
                    .colorSnapshot(pd.getColor() != null ? pd.getColor().getName() : null)
                    .sizeSnapshot(pd.getSize() != null ? pd.getSize().getName() : null)
                    .priceSnapshot(pd.getPrice())
                    .quantity(item.getQuantity())
                    .returnQuantity(0)
                    .totalAmount(lineTotal)
                    .build();
            savedDetails.add(detail);
        }

        BigDecimal shippingFee = bill.getInvoiceType() == 2
                ? BigDecimal.ZERO
                : (subtotal.compareTo(BigDecimal.valueOf(500_000)) < 0
                ? BigDecimal.valueOf(30_000)
                : BigDecimal.ZERO);

        BigDecimal discountAmount = BigDecimal.ZERO;
        Integer discountCodeId = null;
        if (discountCode != null && !discountCode.isBlank()) {
            discountAmount = discountCodeService.validate(discountCode, customerId, subtotal);
            discountCodeId = discountCodeRepository.findByCode(discountCode)
                    .orElseThrow(() -> new ResourceNotFoundException("voucher", discountCode))
                    .getId();
        }

        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(discountAmount);

        bill.setSubtotal(subtotal);
        bill.setShippingFee(shippingFee);
        bill.setDiscountAmount(discountAmount);
        bill.setTotalAmount(totalAmount);
        if (discountCodeId != null) {
            bill.setDiscountCode(DiscountCode.builder().id(discountCodeId).build());
        }
        bill = billRepository.save(bill);

        final Bill savedBill = bill;
        for (BillDetail detail : savedDetails) {
            detail.setBill(savedBill);
            billDetailRepository.save(detail);

            ProductDetail pd = detail.getProductDetail();
            int before = pd.getQuantity();
            int after = before - detail.getQuantity();
            pd.setQuantity(after);
            productDetailRepository.save(pd);

            logInventory(pd, InventoryActionType.SALE,
                    -detail.getQuantity(), before, after, savedBill.getId());
        }

        if (discountCodeId != null && customerId != null) {
            discountCodeService.recordUsage(customerId, discountCodeId, savedBill.getId());
        }

        if (bill.getInvoiceType() == 1) {
            logHistory(savedBill, null, OrderStatus.PENDING.getValue(),
                    "Khách hàng đặt đơn", null);
        }

        log.info("Tạo đơn thành công: {} | total: {}", savedBill.getId(), totalAmount);
        return savedBill;
    }

    // ── Đổi trạng thái ────────────────────────────────────────────────────

    @Override
    @Transactional
    public Bill confirm(String billId, String staffId, String note) {
        Bill bill = findById(billId);
        validateStatus(bill, OrderStatus.PENDING.getValue(), "xác nhận");
        return changeStatus(bill, OrderStatus.CONFIRMED.getValue(), staffId,
                note != null ? note : "Xác nhận đơn hàng");
    }

    @Override
    @Transactional
    public Bill startShipping(String billId, String staffId, String note) {
        Bill bill = findById(billId);
        validateStatus(bill, OrderStatus.CONFIRMED.getValue(), "giao hàng");
        return changeStatus(bill, OrderStatus.SHIPPING.getValue(), staffId,
                note != null ? note : "Bắt đầu giao hàng");
    }

    @Override
    @Transactional
    public Bill markDelivered(String billId, String staffId, String note) {
        Bill bill = findById(billId);
        validateStatus(bill, OrderStatus.SHIPPING.getValue(), "xác nhận đã giao");
        bill = changeStatus(bill, OrderStatus.DELIVERED.getValue(), staffId,
                note != null ? note : "Giao hàng thành công");

        if (bill.getPaymentStatus() == PaymentStatus.UNPAID.getValue()) {
            bill.setPaymentStatus(PaymentStatus.PAID.getValue());
            bill = billRepository.save(bill);
        }

        if (bill.getCustomer() != null) {
            notificationService.sendToCustomer(
                    bill.getCustomer().getId(),
                    "Đơn hàng đã giao thành công",
                    "Đơn " + bill.getId() + " đã được giao. Cảm ơn bạn đã mua hàng!",
                    NotificationType.ORDER_DELIVERED.getValue(),
                    bill.getId()
            );
        }
        return bill;
    }

    @Override
    @Transactional
    public Bill cancel(String billId, String actorId, String note) {
        Bill bill = findById(billId);
        if (bill.getStatus() != OrderStatus.PENDING.getValue()
                && bill.getStatus() != OrderStatus.CONFIRMED.getValue()) {
            throw new AppException("Không thể hủy đơn hàng ở trạng thái hiện tại");
        }

        bill = changeStatus(bill, OrderStatus.CANCELLED.getValue(), actorId,
                note != null ? note : "Hủy đơn hàng");

        List<BillDetail> details = billDetailRepository.findByBillId(billId);
        for (BillDetail detail : details) {
            ProductDetail pd = detail.getProductDetail();
            if (pd == null) continue;
            int before = pd.getQuantity();
            int after = before + detail.getQuantity();
            pd.setQuantity(after);
            productDetailRepository.save(pd);
            logInventory(pd, InventoryActionType.ADJUSTMENT,
                    detail.getQuantity(), before, after, billId);
        }

        log.info("Hủy đơn: {} | actor: {}", billId, actorId);
        return bill;
    }

    @Override
    @Transactional
    public Bill complete(String billId, String staffId, String note) {
        Bill bill = findById(billId);
        validateStatus(bill, OrderStatus.DELIVERED.getValue(), "hoàn thành");
        return changeStatus(bill, OrderStatus.COMPLETED.getValue(), staffId,
                note != null ? note : "Hoàn thành đơn hàng");
    }

    // ── Thanh toán ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void recordPayment(String billId, String transactionCode,
                              String paymentMethod, String gatewayResponse) {
        Bill bill = findById(billId);

        PaymentTransaction tx = PaymentTransaction.builder()
                .bill(bill)
                .transactionCode(transactionCode)
                .amount(bill.getTotalAmount())
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PAID.getValue())
                .gatewayResponse(gatewayResponse)
                .build();
        paymentTransactionRepository.save(tx);

        bill.setPaymentStatus(PaymentStatus.PAID.getValue());
        billRepository.save(bill);

        log.info("Ghi nhận thanh toán: {} | method: {} | txCode: {}",
                billId, paymentMethod, transactionCode);
    }

    // ── Helper ────────────────────────────────────────────────────────────

    // ✅ CHỈ GIỮ METHOD NÀY - XÓA METHOD CÙNG TÊN Ở TRÊN
    private Bill changeStatus(Bill bill, int newStatus, String staffId, String note) {
        int oldStatus = bill.getStatus();

        if (!AllowedTransition.isAllowed(oldStatus, newStatus)) {
            throw new AppException(String.format(
                    "Không thể chuyển đơn hàng từ trạng thái '%s' sang '%s'",
                    OrderStatus.of(oldStatus) != null ? OrderStatus.of(oldStatus).getLabel() : oldStatus,
                    OrderStatus.of(newStatus) != null ? OrderStatus.of(newStatus).getLabel() : newStatus
            ));
        }

        if (newStatus == OrderStatus.COMPLETED.getValue()
                && bill.getPaymentStatus() == PaymentStatus.UNPAID.getValue()
                && bill.getInvoiceType() != 2) {
            throw new AppException("Không thể hoàn thành đơn hàng chưa thanh toán");
        }

        bill.setStatus(newStatus);
        bill.setUpdatedBy(staffId);
        bill = billRepository.save(bill);

        logHistory(bill, oldStatus, newStatus, note, staffId); // ✅ staffId hợp lệ
        return bill;
    }

    private void logHistory(Bill bill, Integer oldStatus, int newStatus,
                            String note, String staffId) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .bill(bill)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .note(note)
                .build();

        // Không set staff để tránh lỗi TransientPropertyValueException
        // staffId đã được lưu trong bill.updatedBy

        orderStatusHistoryRepository.save(history);
    }

    private void logInventory(ProductDetail pd, InventoryActionType type,
                              int change, int before, int after, String referenceId) {
        InventoryTransaction tx = InventoryTransaction.builder()
                .productDetail(pd)
                .type(type.getValue())
                .quantityChange(change)
                .quantityBefore(before)
                .quantityAfter(after)
                .referenceId(referenceId)
                .build();
        inventoryTransactionRepository.save(tx);
    }

    private void validateStatus(Bill bill, int expectedStatus, String action) {
        if (bill.getStatus() != expectedStatus) {
            throw new AppException("Không thể " + action
                    + " đơn hàng ở trạng thái hiện tại");
        }
    }

    @Override
    public Page<Bill> findAllWithCustomer(Pageable pageable) {
        return billRepository.findAllWithCustomer(pageable);
    }

    @Override
    public Page<Bill> findByStatusWithCustomer(Integer status, Pageable pageable) {
        return billRepository.findByStatusWithCustomer(status, pageable);
    }
}