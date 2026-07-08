package com.example.skysport1.service;

import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.BillDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    Bill cancel(String billId, String actorId, String note);

    Bill complete(String billId, String staffId, String note);

    // ── Thanh toán ────────────────────────────────────────

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
}