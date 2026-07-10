package com.example.skysport1.service;

import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.BillDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BillService {

    // ── Query ──────────────────────────────────────────────

    Bill findById(String id);

    List<Bill> findByCustomerId(String customerId);

    List<Bill> findByStatus(Integer status);

    List<Bill> findByGuestEmail(String guestEmail);

    List<Bill> findAll();

    Page<Bill> findAllPaged(Pageable pageable);

    Page<Bill> findByStatusPaged(Integer status, Pageable pageable);


    // ── Tạo đơn ───────────────────────────────────────────

    /**
     * Tạo đơn hàng online cho customer đã đăng nhập.
     * Tự động: trừ tồn kho, log InventoryTransaction, lưu BillDetail snapshot,
     * validate + áp dụng voucher, log OrderStatusHistory.
     */
    Bill createOnlineBill(String customerId,
                           String shippingAddress,
                           String receiverName,
                           String receiverPhone,
                           String paymentId,
                           String discountCode,
                           List<BillDetail> items);

    /**
     * Tạo đơn hàng cho khách vãng lai (guest checkout).
     */
    Bill createGuestBill(String guestEmail,
                          String shippingAddress,
                          String receiverName,
                          String receiverPhone,
                          String paymentId,
                          String discountCode,
                          List<BillDetail> items);

    /**
     * Tạo đơn hàng tại quầy (invoice_type = 2).
     */
    Bill createCounterBill(String customerId,
                             String paymentId,
                             String staffId,
                             List<BillDetail> items);

    // ── Đổi trạng thái ────────────────────────────────────

    Bill confirm(String billId, String staffId, String note);

    Bill startShipping(String billId, String staffId, String note);

    Bill markDelivered(String billId, String staffId, String note);

    /**
     * Hủy bởi staff/admin (giữ nguyên luồng cũ).
     */
    Bill cancel(String billId, String actorId, String note);

    /**
     * Hủy bởi staff/admin sau khi customer đã request cancel.
     */
    Bill approveCancelRequest(String billId, String staffId, String staffNote);

    /**
     * Customer xin hủy (chuyển trạng thái CONFIRMED -> CANCEL_REQUESTED).
     */
    Bill requestCancel(String billId, String customerReason);

    /**
     * Staff từ chối request hủy (chuyển CANCEL_REQUESTED -> CONFIRMED).
     */
    Bill rejectCancelRequest(String billId, String staffId, String staffNote);

    /**
     * Hủy 1 sản phẩm (BillDetail) theo cấp độ:
     * - Nếu bill.status == PENDING(1) hoặc CONFIRMED(2): đặt item_status -> 2 (Đã hủy)
     *   (riêng bill.status == CONFIRMED(2) nên dùng requestCancelBillDetail theo spec thực tế của bạn)
     */
    Bill cancelBillDetail(String billId, Integer billDetailId, String note);

    /**
     * Customer/guest request hủy 1 sản phẩm khi bill.status == CONFIRMED(2)
     * itemStatus: 1 -> 3
     */
    Bill requestCancelBillDetail(String billId, Integer billDetailId, String customerNote);

    /**
     * Staff duyệt yêu cầu hủy 1 sản phẩm
     * itemStatus: 3 -> 2, hoàn kho + tính lại bill subtotal/totalAmount
     */
    Bill approveCancelBillDetail(String billId, Integer billDetailId, String staffId, String staffNote);

    /**
     * Staff từ chối yêu cầu hủy 1 sản phẩm
     * itemStatus: 3 -> 1
     */
    Bill rejectCancelBillDetail(String billId, Integer billDetailId, String staffId, String staffNote);

    Bill complete(String billId, String staffId, String note);

    // ── Thanh toán ───────────────────────────────────────

    void recordPayment(String billId, String transactionCode,
                        String paymentMethod, String gatewayResponse);

    /**
     * Dùng cho các luồng bên ngoài (vd: ReturnRequest) để cập nhật Bill.status
     * và đồng thời log OrderStatusHistory.
     */
    Bill changeBillStatusAndLogHistory(String billId, int newStatus,
                                         String actorAccountId, String note);

    Page<Bill> findAllWithCustomer(Pageable pageable);

    Page<Bill> findByStatusWithCustomer(Integer status, Pageable pageable);

    /**
     * Guest track theo contact (email hoặc phone).
     * Chỉ lấy bill của khách vãng lai thật (customer IS NULL).
     */
    @Query("SELECT b FROM Bill b WHERE b.customer IS NULL " +
            "AND (b.guestEmail = :contact OR b.receiverPhone = :contact) " +
            "ORDER BY b.createDate DESC")
    List<Bill> findGuestBillsByContact(@Param("contact") String contact);
}