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
import com.example.skysport1.service.StaffService;
import com.example.skysport1.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final StaffService staffService;
    private final IdGenerator idGenerator;

    // ── Enum quản lý chuyển trạng thái ────────────────────────────────────
    private enum AllowedTransition {
        PENDING_TO_CONFIRMED(OrderStatus.PENDING.getValue(), OrderStatus.CONFIRMED.getValue()),
        CONFIRMED_TO_SHIPPING(OrderStatus.CONFIRMED.getValue(), OrderStatus.SHIPPING.getValue()),
        SHIPPING_TO_DELIVERED(OrderStatus.SHIPPING.getValue(), OrderStatus.DELIVERED.getValue()),
        DELIVERED_TO_COMPLETED(OrderStatus.DELIVERED.getValue(), OrderStatus.COMPLETED.getValue()),
        PENDING_TO_CANCELLED(OrderStatus.PENDING.getValue(), OrderStatus.CANCELLED.getValue()),
        CONFIRMED_TO_CANCELLED(OrderStatus.CONFIRMED.getValue(), OrderStatus.CANCELLED.getValue()),
        SHIPPING_TO_RETURNING(OrderStatus.SHIPPING.getValue(), OrderStatus.RETURNING.getValue()),
        COMPLETED_TO_RETURNING(OrderStatus.COMPLETED.getValue(), OrderStatus.RETURNING.getValue()),

        // request cancel flow
        CONFIRMED_TO_CANCEL_REQUESTED(OrderStatus.CONFIRMED.getValue(), OrderStatus.CANCEL_REQUESTED.getValue()),
        CANCEL_REQUESTED_TO_CANCELLED(OrderStatus.CANCEL_REQUESTED.getValue(), OrderStatus.CANCELLED.getValue()),
        CANCEL_REQUESTED_TO_CONFIRMED(OrderStatus.CANCEL_REQUESTED.getValue(), OrderStatus.CONFIRMED.getValue());

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

    // ── Query ─────────────────────────────────────────────────────────────

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
    public List<Bill> findGuestBillsByContact(String contact) {
        return billRepository.findGuestBillsByContact(contact);
    }

    @Override
    public List<Bill> findAll() {
        return billRepository.findAll();
    }

    @Override
    public Page<Bill> findAllPaged(Pageable pageable) {
        return billRepository.findAllWithCustomer(pageable);
    }

    // ── Tạo đơn ──────────────────────────────────────────────────────────

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
            // Khoá dòng tồn kho (SELECT ... FOR UPDATE) ngay khi đọc, giữ khoá
            // xuyên suốt transaction cho tới khi trừ kho ở dưới, để 2 đơn đặt
            // cùng sản phẩm gần như đồng thời không thể cùng pass bước kiểm
            // tra tồn kho trước khi bên nào trừ kho xong (tránh oversell).
            ProductDetail pd = productDetailRepository
                    .findByIdForUpdate(item.getProductDetail().getId())
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
        DiscountCode lockedDiscountCode = null;
        if (discountCode != null && !discountCode.isBlank()) {
            // Khoá dòng discount_code (SELECT ... FOR UPDATE) ngay khi đọc, giữ
            // khoá xuyên suốt transaction tới lúc tăng usedCount ở dưới, để 2
            // đơn cùng dùng 1 voucher sắp hết lượt gần như đồng thời không thể
            // cùng pass bước kiểm tra lượt dùng trước khi bên nào tăng usedCount
            // xong (tránh oversell voucher) — đối xứng với cách trừ tồn kho ở
            // trên. Không dùng validate() (không khoá) ở đây vì đây là luồng
            // tạo đơn thật sự, không phải preview.
            lockedDiscountCode = discountCodeService.lockAndValidate(discountCode, customerId, subtotal);
            discountAmount = discountCodeService.calculateDiscountAmount(lockedDiscountCode, subtotal);
            discountCodeId = lockedDiscountCode.getId();
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

        if (discountCodeId != null) {
            if (customerId != null) {
                // Customer đã đăng nhập: ghi nhận CustomerDiscount (chặn dùng lại)
                // + tăng usedCount toàn cục.
                discountCodeService.recordUsage(customerId, lockedDiscountCode, savedBill.getId());
            } else {
                // Guest: không thể tạo CustomerDiscount (customer_id NOT NULL ở DB),
                // nhưng vẫn phải tăng usedCount toàn cục để không bị dùng vô hạn lần.
                discountCodeService.incrementUsedCount(lockedDiscountCode);
            }
        }

        if (bill.getInvoiceType() == 1) {
            logHistory(savedBill, null, OrderStatus.PENDING.getValue(),
                    "Khách hàng đặt đơn", null);
        }

        log.info("Tạo đơn thành công: {} | total: {}", savedBill.getId(), totalAmount);
        return savedBill;
    }

    // ── Đổi trạng thái ───────────────────────────────────────────────────

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

        restoreInventoryForBillCancellation(billId);
        bill = refundIfPaid(bill, "Hoàn tiền do hủy đơn " + billId);

        log.info("Hủy đơn: {} | actor: {}", billId, actorId);
        return bill;
    }

    /**
     * Nếu đơn đã thanh toán (PAID), đánh dấu REFUNDED và tạo một
     * PaymentTransaction ghi nhận việc hoàn tiền toàn bộ. Không làm gì
     * nếu đơn chưa thanh toán, tránh tạo bản ghi hoàn tiền sai lệch.
     */
    private Bill refundIfPaid(Bill bill, String reason) {
        if (bill.getPaymentStatus() == null
                || !PaymentStatus.PAID.matches(bill.getPaymentStatus())) {
            return bill;
        }

        PaymentTransaction tx = PaymentTransaction.builder()
                .bill(bill)
                .transactionCode(null)
                .amount(bill.getTotalAmount() != null ? bill.getTotalAmount() : BigDecimal.ZERO)
                .paymentMethod("REFUND")
                .paymentStatus(PaymentStatus.REFUNDED.getValue())
                .gatewayResponse(reason)
                .build();
        paymentTransactionRepository.save(tx);

        bill.setPaymentStatus(PaymentStatus.REFUNDED.getValue());
        bill = billRepository.save(bill);

        log.info("Hoàn tiền toàn bộ cho đơn {} | số tiền: {}", bill.getId(), tx.getAmount());
        return bill;
    }

    /**
     * Hoàn tiền một phần khi hủy một sản phẩm trong đơn đã thanh toán.
     * Giữ nguyên paymentStatus = PAID vì các sản phẩm khác trong đơn
     * vẫn còn hiệu lực (chưa hoàn toàn bộ đơn).
     */
    private void partialRefundIfPaid(Bill bill, BillDetail detail, String reason) {
        if (bill.getPaymentStatus() == null
                || !PaymentStatus.PAID.matches(bill.getPaymentStatus())) {
            return;
        }

        BigDecimal refundAmount = detail.getTotalAmount() != null ? detail.getTotalAmount() : BigDecimal.ZERO;
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        PaymentTransaction tx = PaymentTransaction.builder()
                .bill(bill)
                .transactionCode(null)
                .amount(refundAmount)
                .paymentMethod("REFUND")
                .paymentStatus(PaymentStatus.REFUNDED.getValue())
                .gatewayResponse(reason)
                .build();
        paymentTransactionRepository.save(tx);

        log.info("Hoàn tiền một phần cho đơn {} | billDetailId: {} | số tiền: {}",
                bill.getId(), detail.getId(), refundAmount);
    }

    // ── New flow: customer request cancel (A) ───────────────────────────

    @Override
    @Transactional
    public Bill requestCancel(String billId, String customerReason) {
        Bill bill = findById(billId);

        validateStatus(bill, OrderStatus.CONFIRMED.getValue(), "yêu cầu hủy");

        return changeStatus(
                bill,
                OrderStatus.CANCEL_REQUESTED.getValue(),
                null,
                customerReason != null ? customerReason : "Khách hàng yêu cầu hủy đơn"
        );
    }

    @Override
    @Transactional
    public Bill approveCancelRequest(String billId, String staffId, String staffNote) {
        Bill bill = findById(billId);

        validateStatus(bill, OrderStatus.CANCEL_REQUESTED.getValue(), "duyệt yêu cầu hủy");

        bill = changeStatus(
                bill,
                OrderStatus.CANCELLED.getValue(),
                staffId,
                staffNote != null ? staffNote : "Duyệt hủy đơn"
        );

        restoreInventoryForBillCancellation(billId);
        bill = refundIfPaid(bill, "Hoàn tiền do duyệt hủy đơn " + billId);

        log.info("Approve cancel request: {} | staff: {}", billId, staffId);
        return bill;
    }

    @Override
    @Transactional
    public Bill rejectCancelRequest(String billId, String staffId, String staffNote) {
        Bill bill = findById(billId);

        validateStatus(bill, OrderStatus.CANCEL_REQUESTED.getValue(), "từ chối yêu cầu hủy");

        bill = changeStatus(
                bill,
                OrderStatus.CONFIRMED.getValue(),
                staffId,
                staffNote != null ? staffNote : "Từ chối hủy đơn"
        );

        log.info("Reject cancel request: {} | staff: {}", billId, staffId);
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

    private void restoreInventoryForBillCancellation(String billId) {
        List<BillDetail> details = billDetailRepository.findByBillId(billId);
        for (BillDetail detail : details) {
            // Bỏ qua các item đã bị hủy riêng lẻ trước đó (đã hoàn kho rồi)
            // để tránh hoàn kho 2 lần khi hủy cả đơn.
            if (detail.getItemStatus() != null && detail.getItemStatus() == ITEM_STATUS_CANCELLED) {
                continue;
            }

            ProductDetail pd = detail.getProductDetail();
            if (pd != null) {
                int before = pd.getQuantity();
                int after = before + detail.getQuantity();
                pd.setQuantity(after);
                productDetailRepository.save(pd);

                logInventory(pd, InventoryActionType.ADJUSTMENT,
                        detail.getQuantity(), before, after, billId);
            }

            // Đồng bộ item_status để dữ liệu nhất quán với trạng thái CANCELLED của cả đơn
            if (detail.getItemStatus() == null || detail.getItemStatus() != ITEM_STATUS_CANCELLED) {
                detail.setItemStatus(ITEM_STATUS_CANCELLED);
                billDetailRepository.save(detail);
            }
        }
    }

    // ── New flow: cancel 1 item (Option 2) ─────────────────────────────

    private static final int ITEM_STATUS_NORMAL = 1;
    private static final int ITEM_STATUS_CANCELLED = 2;
    private static final int ITEM_STATUS_CANCEL_REQUESTED = 3;

    @Override
    @Transactional
    public Bill cancelBillDetail(String billId, Integer billDetailId, String note) {
        Bill bill = findById(billId);

        if (bill.getStatus() != OrderStatus.PENDING.getValue()
                && bill.getStatus() != OrderStatus.CONFIRMED.getValue()) {
            throw new AppException("Không thể hủy sản phẩm ở trạng thái hiện tại");
        }

        BillDetail detail = findBillDetail(billId, billDetailId);
        validateItemBelongsToBill(detail, billId);
        validateItemStatus(detail, ITEM_STATUS_NORMAL);

        validateNotCancelAll(billId, detail.getId());

        if (bill.getStatus() == OrderStatus.PENDING.getValue()) {
            partialRefundIfPaid(bill, detail, "Hoàn tiền do hủy sản phẩm trong đơn " + billId);

            detail.setItemStatus(ITEM_STATUS_CANCELLED);
            billDetailRepository.save(detail);

            restoreInventoryForBillDetailCancellation(detail, billId);
            recalcBillTotalsExcludingCancelledItems(bill);

            log.info("Cancel bill detail directly: billId={}, billDetailId={}", billId, billDetailId);
            logItemCancelHistory(bill, detail, ITEM_STATUS_NORMAL, ITEM_STATUS_CANCELLED, note, null);
            return billRepository.save(bill);
        }

        return requestCancelBillDetail(billId, billDetailId, note);
    }

    @Override
    @Transactional
    public Bill requestCancelBillDetail(String billId, Integer billDetailId, String customerNote) {
        Bill bill = findById(billId);

        validateStatus(bill, OrderStatus.CONFIRMED.getValue(), "yêu cầu hủy sản phẩm");

        BillDetail detail = findBillDetail(billId, billDetailId);
        validateItemBelongsToBill(detail, billId);
        validateItemStatus(detail, ITEM_STATUS_NORMAL);
        validateNotCancelAll(billId, detail.getId());

        detail.setItemStatus(ITEM_STATUS_CANCEL_REQUESTED);
        billDetailRepository.save(detail);

        logItemCancelHistory(
                bill,
                detail,
                ITEM_STATUS_NORMAL,
                ITEM_STATUS_CANCEL_REQUESTED,
                customerNote != null ? customerNote : "Khách hàng yêu cầu hủy sản phẩm",
                null
        );

        log.info("Request cancel bill detail: billId={}, billDetailId={}", billId, billDetailId);
        return bill;
    }

    @Override
    @Transactional
    public Bill approveCancelBillDetail(String billId, Integer billDetailId, String staffId, String staffNote) {
        Bill bill = findById(billId);

        BillDetail detail = findBillDetail(billId, billDetailId);
        validateItemBelongsToBill(detail, billId);
        validateItemStatus(detail, ITEM_STATUS_CANCEL_REQUESTED);

        partialRefundIfPaid(bill, detail, "Hoàn tiền do duyệt hủy sản phẩm trong đơn " + billId);

        detail.setItemStatus(ITEM_STATUS_CANCELLED);
        billDetailRepository.save(detail);

        restoreInventoryForBillDetailCancellation(detail, billId);
        recalcBillTotalsExcludingCancelledItems(bill);

        logItemCancelHistory(
                bill,
                detail,
                ITEM_STATUS_CANCEL_REQUESTED,
                ITEM_STATUS_CANCELLED,
                staffNote != null ? staffNote : "Staff duyệt hủy sản phẩm",
                staffId
        );

        log.info("Approve cancel bill detail: billId={}, billDetailId={}, staff={}", billId, billDetailId, staffId);
        return billRepository.save(bill);
    }

    @Override
    @Transactional
    public Bill rejectCancelBillDetail(String billId, Integer billDetailId, String staffId, String staffNote) {
        Bill bill = findById(billId);

        BillDetail detail = findBillDetail(billId, billDetailId);
        validateItemBelongsToBill(detail, billId);
        validateItemStatus(detail, ITEM_STATUS_CANCEL_REQUESTED);

        detail.setItemStatus(ITEM_STATUS_NORMAL);
        billDetailRepository.save(detail);

        logItemCancelHistory(
                bill,
                detail,
                ITEM_STATUS_CANCEL_REQUESTED,
                ITEM_STATUS_NORMAL,
                staffNote != null ? staffNote : "Staff từ chối hủy sản phẩm",
                staffId
        );

        log.info("Reject cancel bill detail: billId={}, billDetailId={}, staff={}", billId, billDetailId, staffId);
        return bill;
    }

    private BillDetail findBillDetail(String billId, Integer billDetailId) {
        return billDetailRepository.findById(billDetailId)
                .orElseThrow(() -> new ResourceNotFoundException("billDetail", String.valueOf(billDetailId)));
    }

    private void validateItemBelongsToBill(BillDetail detail, String billId) {
        if (detail.getBill() == null || detail.getBill().getId() == null) {
            throw new AppException("BillDetail không thuộc đơn hợp lệ");
        }
        if (!billId.equals(detail.getBill().getId())) {
            throw new AppException("BillDetail không thuộc đơn hàng");
        }
    }

    private void validateItemStatus(BillDetail detail, int expected) {
        Integer current = detail.getItemStatus();
        if (current == null) {
            throw new AppException("Trạng thái sản phẩm không hợp lệ");
        }
        if (current != expected) {
            throw new AppException("Sản phẩm không ở trạng thái cho phép");
        }
    }

    private void validateNotCancelAll(String billId, Integer cancelingBillDetailId) {
        List<BillDetail> details = billDetailRepository.findByBillId(billId);
        long remainingNormal = details.stream()
                .filter(d -> d.getId() != null && !d.getId().equals(cancelingBillDetailId))
                .filter(d -> d.getItemStatus() != null && d.getItemStatus() == ITEM_STATUS_NORMAL)
                .count();

        if (remainingNormal < 1) {
            throw new AppException("Không thể hủy hết sản phẩm trong đơn");
        }
    }

    private void restoreInventoryForBillDetailCancellation(BillDetail detail, String referenceId) {
        ProductDetail pd = detail.getProductDetail();
        if (pd == null) return;

        int before = pd.getQuantity();
        int after = before + detail.getQuantity();
        pd.setQuantity(after);
        productDetailRepository.save(pd);

        logInventory(pd, InventoryActionType.ADJUSTMENT,
                detail.getQuantity(), before, after, referenceId);
    }

    private void recalcBillTotalsExcludingCancelledItems(Bill bill) {
        List<BillDetail> details = billDetailRepository.findByBillId(bill.getId());

        BigDecimal newSubtotal = BigDecimal.ZERO;
        for (BillDetail d : details) {
            if (d.getItemStatus() != null && d.getItemStatus() == ITEM_STATUS_NORMAL) {
                if (d.getTotalAmount() != null) {
                    newSubtotal = newSubtotal.add(d.getTotalAmount());
                }
            }
        }

        BigDecimal newShippingFee;
        if (bill.getInvoiceType() == 2) {
            newShippingFee = BigDecimal.ZERO;
        } else {
            newShippingFee = (newSubtotal.compareTo(BigDecimal.valueOf(500_000)) < 0)
                    ? BigDecimal.valueOf(30_000)
                    : BigDecimal.ZERO;
        }

        BigDecimal newDiscountAmount = bill.getDiscountAmount() != null ? bill.getDiscountAmount() : BigDecimal.ZERO;

        BigDecimal newTotal = newSubtotal.add(newShippingFee).subtract(newDiscountAmount);

        bill.setSubtotal(newSubtotal);
        bill.setShippingFee(newShippingFee);
        bill.setDiscountAmount(newDiscountAmount);
        bill.setTotalAmount(newTotal);

        billRepository.save(bill);
    }

    // ── Thanh toán ───────────────────────────────────────────────────────

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

    // ── Helper ───────────────────────────────────────────────────────────

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

        logHistory(bill, oldStatus, newStatus, note, staffId);
        return bill;
    }

    private void logHistory(Bill bill, Integer oldStatus, int newStatus,
                            String note, String staffId) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .bill(bill)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .note(note)
                .createDate(LocalDateTime.now())
                .build();

        if (staffId != null) {
            Staff staff = staffService.findByAccountId(staffId);
            history.setStaff(staff);
        }

        orderStatusHistoryRepository.save(history);
    }

    private void logItemCancelHistory(Bill bill, BillDetail detail,
                                      int fromItemStatus, int toItemStatus,
                                      String note, String staffId) {
        String prefix = staffId != null ? "Staff" : "Customer";
        String message = prefix + " cập nhật sản phẩm (billDetailId=" + detail.getId()
                + "): " + fromItemStatus + " -> " + toItemStatus + ". " + note;

        OrderStatusHistory history = OrderStatusHistory.builder()
                .bill(bill)
                .oldStatus(bill.getStatus())
                .newStatus(bill.getStatus())
                .note(message)
                .createDate(LocalDateTime.now())
                .build();

        if (staffId != null) {
            Staff staff = staffService.findByAccountId(staffId);
            history.setStaff(staff);
        }
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
            throw new AppException("Không thể " + action + " đơn hàng ở trạng thái hiện tại");
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

    @Override
    @Transactional
    public Bill changeBillStatusAndLogHistory(String billId, int newStatus,
                                              String actorAccountId, String note) {
        Bill bill = findById(billId);

        int oldStatus = bill.getStatus();
        if (!AllowedTransition.isAllowed(oldStatus, newStatus)) {
            throw new AppException(String.format(
                    "Không thể chuyển đơn hàng từ trạng thái '%s' sang '%s'",
                    OrderStatus.of(oldStatus) != null ? OrderStatus.of(oldStatus).getLabel() : oldStatus,
                    OrderStatus.of(newStatus) != null ? OrderStatus.of(newStatus).getLabel() : newStatus
            ));
        }

        bill.setStatus(newStatus);
        bill.setUpdatedBy(actorAccountId);
        bill = billRepository.save(bill);

        logHistory(bill, oldStatus, newStatus, note, actorAccountId);
        return bill;
    }
}