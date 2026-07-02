package com.example.skysport1.service;

import com.example.skysport1.entity.ReturnRequest;
import com.example.skysport1.entity.ReturnRequestDetail;

import java.util.List;

public interface ReturnRequestService {

    // ── Query ──────────────────────────────────────────────

    ReturnRequest findById(String id);

    List<ReturnRequest> findAll();

    List<ReturnRequest> findByBillId(String billId);

    List<ReturnRequest> findByStatus(Integer status);

    List<ReturnRequestDetail> findDetails(String returnRequestId);

    // ── Tạo yêu cầu ───────────────────────────────────────

    /**
     * Khách hàng gửi yêu cầu hoàn trả.
     * Validate: đơn phải ở trạng thái Đã giao (4) hoặc Hoàn thành (7).
     * Mỗi bill chỉ được tạo 1 return request chưa bị từ chối.
     */
    ReturnRequest create(String billId,
                         String reason,
                         String description,
                         String evidenceUrl,
                         List<ReturnRequestDetail> details);

    // ── Đổi trạng thái ────────────────────────────────────

    /**
     * Staff duyệt yêu cầu hoàn trả → đổi bill sang trạng thái Hoàn trả (6)
     * → hoàn lại tồn kho → tạo BillReturn → log history.
     */
    ReturnRequest approve(String returnRequestId, String staffId, String note);

    /**
     * Staff từ chối yêu cầu hoàn trả → log history.
     */
    ReturnRequest reject(String returnRequestId, String staffId, String note);

    /**
     * Xác nhận đã hoàn tiền → đổi sang trạng thái Đã hoàn tiền (4)
     * → cập nhật payment_status bill sang REFUNDED.
     */
    ReturnRequest confirmRefund(String returnRequestId, String staffId, String note);
}