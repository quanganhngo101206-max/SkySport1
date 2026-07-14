package com.example.skysport1.service.impl;

import com.example.skysport1.entity.*;
import com.example.skysport1.enums.ImportOrderStatus;
import com.example.skysport1.enums.InventoryActionType;
import com.example.skysport1.enums.NotificationType;
import com.example.skysport1.exception.AppException;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.*;
import com.example.skysport1.service.ImportOrderService;
import com.example.skysport1.service.NotificationService;
import com.example.skysport1.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportOrderServiceImpl implements ImportOrderService {

    private final ImportOrderRepository          importOrderRepository;
    private final ImportOrderDetailRepository    importOrderDetailRepository;
    private final ImportStatusHistoryRepository  importStatusHistoryRepository;
    private final ProductDetailRepository        productDetailRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final IdGenerator                    idGenerator;
    private final NotificationService             notificationService;

    // ── Query ──────────────────────────────────────────────────────────────

    @Override
    public ImportOrder findById(String id) {
        return importOrderRepository.findByIdWithSupplier(id)
                .orElseThrow(() -> new ResourceNotFoundException("phiếu nhập", id));
    }

    @Override
    public List<ImportOrder> findAll() {
        return importOrderRepository.findAll();
    }

    @Override
    public List<ImportOrder> findByStatus(Integer status) {
        return importOrderRepository.findByStatus(status);
    }

    @Override
    public List<ImportOrder> findBySupplierId(String supplierId) {
        return importOrderRepository.findBySupplierId(supplierId);
    }

    @Override
    public List<ImportOrder> findByStaffId(String staffId) {
        return importOrderRepository.findByStaffId(staffId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportOrderDetail> findDetails(String importOrderId) {
        return importOrderDetailRepository.findByImportOrderId(importOrderId);
    }

    // ── Tạo phiếu ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ImportOrder create(String supplierId, String staffId, String note,
                              List<ImportOrderDetail> details) {
        if (details == null || details.isEmpty()) {
            throw new AppException("Phiếu nhập phải có ít nhất 1 sản phẩm");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ImportOrderDetail> validatedDetails = new ArrayList<>();

        for (ImportOrderDetail item : details) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new AppException("Số lượng nhập phải lớn hơn 0");
            }

            ProductDetail pd = productDetailRepository
                    .findById(item.getProductDetail().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "sản phẩm", String.valueOf(item.getProductDetail().getId())));

            BigDecimal importPrice = item.getImportPrice() != null
                    ? item.getImportPrice()
                    : pd.getCostPrice() != null ? pd.getCostPrice() : BigDecimal.ZERO;

            BigDecimal lineTotal = importPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            validatedDetails.add(ImportOrderDetail.builder()
                    .productDetail(pd)
                    .quantity(item.getQuantity())
                    .importPrice(importPrice)
                    .totalAmount(lineTotal)
                    .build());
        }

        ImportOrder importOrder = ImportOrder.builder()
                .id(idGenerator.generateImportOrderId())
                .supplier(supplierId != null ? Supplier.builder().id(supplierId).build() : null)
                .staff(staffId != null ? Staff.builder().id(staffId).build() : null)
                .totalAmount(totalAmount)
                .status(ImportOrderStatus.PENDING.getValue())
                .note(note)
                .createdBy(staffId)
                .build();
        importOrder = importOrderRepository.save(importOrder);

        final ImportOrder savedOrder = importOrder;
        for (ImportOrderDetail detail : validatedDetails) {
            detail.setImportOrder(savedOrder);
            importOrderDetailRepository.save(detail);
        }

        logHistory(savedOrder, null, ImportOrderStatus.PENDING.getValue(),
                "Tạo phiếu nhập", staffId);

        // Báo cho admin biết có phiếu nhập mới đang chờ duyệt — hạ tầng
        // notification đã có sẵn (enum NEW_IMPORT_ORDER), chỉ còn thiếu bước
        // "nối dây" này. Lỗi gửi thông báo (nếu có) chỉ log bên trong
        // notifyAllAdmins, không làm rollback việc tạo phiếu nhập.
        notificationService.notifyAllAdmins(
                "Phiếu nhập mới",
                "Phiếu nhập " + savedOrder.getId() + " vừa được tạo, tổng tiền "
                        + totalAmount + "đ — đang chờ duyệt.",
                NotificationType.NEW_IMPORT_ORDER.getValue(),
                savedOrder.getId());

        log.info("Tạo phiếu nhập: {} | supplier: {} | total: {}",
                savedOrder.getId(), supplierId, totalAmount);
        return savedOrder;
    }

    // ── Sửa phiếu (chỉ khi đang Chờ duyệt) ──────────────────────────────────

    @Override
    @Transactional
    public ImportOrder update(String importOrderId, String supplierId, String note,
                              List<ImportOrderDetail> details, String staffId) {
        ImportOrder importOrder = findById(importOrderId);

        if (importOrder.getStatus() != ImportOrderStatus.PENDING.getValue()) {
            throw new AppException("Chỉ có thể sửa phiếu nhập đang ở trạng thái Chờ duyệt");
        }
        if (details == null || details.isEmpty()) {
            throw new AppException("Phiếu nhập phải có ít nhất 1 sản phẩm");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ImportOrderDetail> validatedDetails = new ArrayList<>();

        for (ImportOrderDetail item : details) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new AppException("Số lượng nhập phải lớn hơn 0");
            }

            ProductDetail pd = productDetailRepository
                    .findById(item.getProductDetail().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "sản phẩm", String.valueOf(item.getProductDetail().getId())));

            BigDecimal importPrice = item.getImportPrice() != null
                    ? item.getImportPrice()
                    : pd.getCostPrice() != null ? pd.getCostPrice() : BigDecimal.ZERO;

            BigDecimal lineTotal = importPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            validatedDetails.add(ImportOrderDetail.builder()
                    .productDetail(pd)
                    .quantity(item.getQuantity())
                    .importPrice(importPrice)
                    .totalAmount(lineTotal)
                    .build());
        }

        // Xoá hết chi tiết cũ rồi ghi lại chi tiết mới — an toàn vì phiếu
        // đang Chờ duyệt nghĩa là CHƯA cộng tồn kho, không có gì cần "hoàn tác".
        importOrderDetailRepository.deleteByImportOrderId(importOrderId);

        importOrder.setSupplier(supplierId != null ? Supplier.builder().id(supplierId).build() : null);
        importOrder.setNote(note);
        importOrder.setTotalAmount(totalAmount);
        importOrder.setUpdatedBy(staffId);
        importOrder = importOrderRepository.save(importOrder);

        final ImportOrder savedOrder = importOrder;
        for (ImportOrderDetail detail : validatedDetails) {
            detail.setImportOrder(savedOrder);
            importOrderDetailRepository.save(detail);
        }

        logHistory(savedOrder, savedOrder.getStatus(), savedOrder.getStatus(),
                "Sửa phiếu nhập", staffId);

        log.info("Sửa phiếu nhập: {} | staff: {}", importOrderId, staffId);
        return savedOrder;
    }

    // ── Duyệt ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ImportOrder approve(String importOrderId, String staffId, String note) {
        ImportOrder importOrder = findById(importOrderId);

        if (importOrder.getStatus() != ImportOrderStatus.PENDING.getValue()) {
            throw new AppException("Chỉ có thể duyệt phiếu nhập đang ở trạng thái Chờ duyệt");
        }

        List<ImportOrderDetail> details =
                importOrderDetailRepository.findByImportOrderId(importOrderId);

        for (ImportOrderDetail detail : details) {
            if (detail.getProductDetail() == null) continue;

            // FIX: load fresh rồi dùng đúng biến freshPd thay vì pd cũ
            ProductDetail freshPd = productDetailRepository
                    .findById(detail.getProductDetail().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "sản phẩm", String.valueOf(detail.getProductDetail().getId())));

            int before = freshPd.getQuantity();
            int after  = before + detail.getQuantity();
            freshPd.setQuantity(after);
            productDetailRepository.save(freshPd);

            logInventory(freshPd, InventoryActionType.IMPORT,
                    detail.getQuantity(), before, after, importOrderId);
        }

        int oldStatus = importOrder.getStatus();
        importOrder.setStatus(ImportOrderStatus.APPROVED.getValue());
        importOrder.setUpdatedBy(staffId);
        importOrder = importOrderRepository.save(importOrder);

        logHistory(importOrder, oldStatus, ImportOrderStatus.APPROVED.getValue(),
                note != null ? note : "Duyệt phiếu nhập — đã cộng tồn kho", staffId);

        log.info("Duyệt phiếu nhập: {} | staff: {}", importOrderId, staffId);
        return importOrder;
    }

    // ── Từ chối ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ImportOrder reject(String importOrderId, String staffId, String note) {
        ImportOrder importOrder = findById(importOrderId);

        if (importOrder.getStatus() != ImportOrderStatus.PENDING.getValue()) {
            throw new AppException("Chỉ có thể từ chối phiếu nhập đang ở trạng thái Chờ duyệt");
        }

        int oldStatus = importOrder.getStatus();
        importOrder.setStatus(ImportOrderStatus.REJECTED.getValue());
        importOrder.setUpdatedBy(staffId);
        importOrder = importOrderRepository.save(importOrder);

        logHistory(importOrder, oldStatus, ImportOrderStatus.REJECTED.getValue(),
                note != null ? note : "Từ chối phiếu nhập", staffId);

        log.info("Từ chối phiếu nhập: {} | staff: {}", importOrderId, staffId);
        return importOrder;
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private void logHistory(ImportOrder importOrder, Integer oldStatus,
                            int newStatus, String note, String staffId) {
        importStatusHistoryRepository.save(ImportStatusHistory.builder()
                .importOrder(importOrder)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .note(note)
                .staff(staffId != null ? Staff.builder().id(staffId).build() : null)
                .build());
    }

    private void logInventory(ProductDetail pd, InventoryActionType type,
                              int change, int before, int after, String referenceId) {
        inventoryTransactionRepository.save(InventoryTransaction.builder()
                .productDetail(pd)
                .type(type.getValue())
                .quantityChange(change)
                .quantityBefore(before)
                .quantityAfter(after)
                .referenceId(referenceId)
                .build());
    }
}