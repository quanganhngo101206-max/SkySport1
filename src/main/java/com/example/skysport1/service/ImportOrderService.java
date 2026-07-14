package com.example.skysport1.service;

import com.example.skysport1.entity.ImportOrder;
import com.example.skysport1.entity.ImportOrderDetail;

import java.util.List;

public interface ImportOrderService {

    // ── Query ──────────────────────────────────────────────

    ImportOrder findById(String id);

    List<ImportOrder> findAll();

    List<ImportOrder> findByStatus(Integer status);

    List<ImportOrder> findBySupplierId(String supplierId);

    List<ImportOrder> findByStaffId(String staffId);

    List<ImportOrderDetail> findDetails(String importOrderId);

    // ── Tạo phiếu ─────────────────────────────────────────

    /**
     * Tạo phiếu nhập kho ở trạng thái Chờ duyệt.
     * Chưa cộng tồn kho — chỉ cộng khi duyệt.
     */
    ImportOrder create(String supplierId, String staffId, String note,
                       List<ImportOrderDetail> details);

    /**
     * Sửa phiếu nhập đang ở trạng thái Chờ duyệt (supplier, ghi chú, danh sách
     * sản phẩm/số lượng/giá nhập). Không cho sửa phiếu đã Duyệt/Từ chối — phiếu
     * Duyệt rồi đã cộng tồn kho, sửa lại sẽ làm sai lệch dữ liệu tồn kho.
     */
    ImportOrder update(String importOrderId, String supplierId, String note,
                       List<ImportOrderDetail> details, String staffId);

    // ── Đổi trạng thái ────────────────────────────────────

    /**
     * Duyệt phiếu nhập → cộng tồn kho + log InventoryTransaction.
     */
    ImportOrder approve(String importOrderId, String staffId, String note);

    /**
     * Từ chối phiếu nhập — không ảnh hưởng tồn kho.
     */
    ImportOrder reject(String importOrderId, String staffId, String note);
}