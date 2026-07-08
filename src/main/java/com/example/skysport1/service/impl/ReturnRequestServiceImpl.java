package com.example.skysport1.service.impl;

import com.example.skysport1.entity.*;
import com.example.skysport1.enums.*;
import com.example.skysport1.exception.AppException;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.*;
import com.example.skysport1.service.BillService;
import com.example.skysport1.service.NotificationService;
import com.example.skysport1.service.ReturnRequestService;
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
public class ReturnRequestServiceImpl implements ReturnRequestService {

    private final ReturnRequestRepository         returnRequestRepository;
    private final ReturnRequestDetailRepository   returnRequestDetailRepository;
    private final ReturnStatusHistoryRepository   returnStatusHistoryRepository;
    private final BillService                     billService;
    private final BillRepository                  billRepository;
    private final BillDetailRepository            billDetailRepository;
    private final BillReturnRepository            billReturnRepository;
    private final ProductDetailRepository         productDetailRepository;
    private final InventoryTransactionRepository  inventoryTransactionRepository;
    private final NotificationService             notificationService;
    private final IdGenerator                     idGenerator;
    private final StaffRepository                staffRepository;

    // ── Query ─────────────────────────────────────────────────────────────

    @Override
    public ReturnRequest findById(String id) {
        return returnRequestRepository.findByIdWithBillAndCustomer(id)
                .orElseThrow(() -> new ResourceNotFoundException("yêu cầu hoàn trả", id));
    }

    @Override
    public List<ReturnRequest> findAll() {
        return returnRequestRepository.findAllWithBillAndCustomer();
    }

    @Override
    public List<ReturnRequest> findByBillId(String billId) {
        return returnRequestRepository.findByBillId(billId);
    }

    @Override
    public List<ReturnRequest> findByStatus(Integer status) {
        return returnRequestRepository.findByStatus(status);
    }

    @Override
    public List<ReturnRequestDetail> findDetails(String returnRequestId) {
        return returnRequestDetailRepository.findByReturnRequestIdWithProductSizeColor(returnRequestId);
    }

    // ── Tạo yêu cầu ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReturnRequest create(String billId,
                                 String reason,
                                 String description,
                                 String evidenceUrl,
                                 List<ReturnRequestDetail> details) {

        // Validate bill tồn tại
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("đơn hàng", billId));

        // Chỉ cho hoàn trả khi đã giao hoặc hoàn thành
        if (bill.getStatus() != OrderStatus.DELIVERED.getValue()
                && bill.getStatus() != OrderStatus.COMPLETED.getValue()) {
            throw new AppException("Chỉ có thể yêu cầu hoàn trả đơn hàng đã giao");
        }

        // Kiểm tra đã có return request đang chờ/đã duyệt chưa
        boolean hasPendingReturn = returnRequestRepository.findByBillId(billId)
                .stream()
                .anyMatch(r -> r.getStatus() == ReturnRequestStatus.PENDING.getValue()
                        || r.getStatus() == ReturnRequestStatus.APPROVED.getValue());
        if (hasPendingReturn) {
            throw new AppException("Đơn hàng này đã có yêu cầu hoàn trả đang xử lý");
        }

        // Validate chi tiết hoàn trả
        if (details == null || details.isEmpty()) {
            throw new AppException("Phải có ít nhất 1 sản phẩm cần hoàn trả");
        }

        // Tính tổng tiền hoàn và validate số lượng
        List<ReturnRequestDetail> validatedDetails = new ArrayList<>();
        for (ReturnRequestDetail item : details) {
            if (item.getQuantityReturn() == null || item.getQuantityReturn() <= 0) {
                throw new AppException("Số lượng hoàn phải lớn hơn 0");
            }

            // Kiểm tra số lượng không vượt quá đã mua
            BillDetail billDetail = billDetailRepository
                    .findByBillIdAndProductDetailId(billId, item.getProductDetail().getId())
                    .orElseThrow(() -> new AppException(
                            "Sản phẩm không thuộc đơn hàng này"));

            int maxReturn = billDetail.getQuantity() - billDetail.getReturnQuantity();
            if (item.getQuantityReturn() > maxReturn) {
                throw new AppException("Số lượng hoàn vượt quá số lượng có thể hoàn (tối đa: "
                        + maxReturn + ")");
            }

            // Tính giá hoàn = giá snapshot * số lượng hoàn
            BigDecimal refundPrice = billDetail.getPriceSnapshot()
                    .multiply(BigDecimal.valueOf(item.getQuantityReturn()));

            ReturnRequestDetail detail = ReturnRequestDetail.builder()
                    .productDetail(item.getProductDetail())
                    .quantityReturn(item.getQuantityReturn())
                    .refundPrice(refundPrice)
                    .build();
            validatedDetails.add(detail);
        }

        // Tạo return request
        ReturnRequest returnRequest = ReturnRequest.builder()
                .id(idGenerator.generateReturnRequestId())
                .bill(bill)
                .reason(reason)
                .description(description)
                .evidenceUrl(evidenceUrl)
                .status(ReturnRequestStatus.PENDING.getValue())
                .build();
        returnRequest = returnRequestRepository.save(returnRequest);

        // Lưu details
        final ReturnRequest savedRR = returnRequest;
        for (ReturnRequestDetail detail : validatedDetails) {
            detail.setReturnRequest(savedRR);
            returnRequestDetailRepository.save(detail);
        }

        // Log history
        logHistory(savedRR, null, ReturnRequestStatus.PENDING.getValue(),
                "Khách gửi yêu cầu hoàn trả", null);

        // Cập nhật bill status → Hoàn trả (RETURNING) + log OrderStatusHistory
        billService.changeBillStatusAndLogHistory(
                billId,
                OrderStatus.RETURNING.getValue(),
                null,
                "Khách gửi yêu cầu hoàn trả"
        );

        log.info("Tạo yêu cầu hoàn trả: {} | bill: {}", savedRR.getId(), billId);
        return savedRR;
    }

    // ── Duyệt ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReturnRequest approve(String returnRequestId, String staffId, String note) {
        ReturnRequest rr = findById(returnRequestId);

        if (rr.getStatus() != ReturnRequestStatus.PENDING.getValue()) {
            throw new AppException("Chỉ có thể duyệt yêu cầu đang Chờ xử lý");
        }

        Bill bill = rr.getBill();
        List<ReturnRequestDetail> details =
                returnRequestDetailRepository.findByReturnRequestId(returnRequestId);

        // Tính tổng tiền hoàn
        BigDecimal totalRefund = BigDecimal.ZERO;

        for (ReturnRequestDetail detail : details) {
            ProductDetail pd = detail.getProductDetail();
            if (pd == null) continue;

            // Load fresh với ID từ detail
            ProductDetail freshPd = productDetailRepository
                    .findById(pd.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "sản phẩm", String.valueOf(pd.getId())));

            // Hoàn lại tồn kho - dùng freshPd để update
            int before = freshPd.getQuantity();
            int after = before + detail.getQuantityReturn();
            freshPd.setQuantity(after);
            productDetailRepository.save(freshPd);

            // Log inventory
            logInventory(freshPd, InventoryActionType.RETURN,
                    detail.getQuantityReturn(), before, after, returnRequestId);

            // Cộng dồn tiền hoàn
            if (detail.getRefundPrice() != null) {
                totalRefund = totalRefund.add(detail.getRefundPrice());
            }

            // Cập nhật return_quantity trong BillDetail
            billDetailRepository.findByBillIdAndProductDetailId(
                            bill.getId(), pd.getId())
                    .ifPresent(bd -> {
                        bd.setReturnQuantity(
                                bd.getReturnQuantity() + detail.getQuantityReturn());
                        billDetailRepository.save(bd);
                    });
        }

        // Đổi trạng thái return request
        int oldStatus = rr.getStatus();
        rr.setStatus(ReturnRequestStatus.APPROVED.getValue());
        rr = returnRequestRepository.save(rr);

        logHistory(rr, oldStatus, ReturnRequestStatus.APPROVED.getValue(),
                note != null ? note : "Duyệt yêu cầu hoàn trả", staffId);

        // Cập nhật bill status (giữ RETURNING) + log OrderStatusHistory
        // (tùy yêu cầu UI; mục tiêu là không thiếu timeline cho admin)
        billService.changeBillStatusAndLogHistory(
                bill.getId(),
                OrderStatus.RETURNING.getValue(),
                staffId,
                note != null ? note : "Duyệt yêu cầu hoàn trả"
        );

        // Tạo BillReturn
        BillReturn billReturn = BillReturn.builder()
                .id(idGenerator.generateBillReturnId())
                .returnRequest(rr)
                .bill(bill)
                .returnReason(rr.getReason())
                .returnMoney(totalRefund)
                .percentFee(BigDecimal.ZERO)
                .isCancel(false)
                .returnStatus(1) // Chờ hoàn tiền
                .createdBy(staffId)
                .build();
        billReturnRepository.save(billReturn);

        // Gửi thông báo cho customer
        if (bill.getCustomer() != null) {
            notificationService.sendToCustomer(
                    bill.getCustomer().getId(),
                    "Yêu cầu hoàn trả đã được duyệt",
                    "Yêu cầu hoàn trả của đơn " + bill.getId()
                            + " đã được chấp nhận. Tiền hoàn " + totalRefund
                            + "đ sẽ về trong 3-5 ngày làm việc.",
                    NotificationType.RETURN_APPROVED.getValue(),
                    rr.getId()
            );
        }

        log.info("Duyệt hoàn trả: {} | refund: {} | staff: {}",
                returnRequestId, totalRefund, staffId);
        return rr;
    }

    // ── Từ chối ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReturnRequest reject(String returnRequestId, String staffId, String note) {
        ReturnRequest rr = findById(returnRequestId);

        if (rr.getStatus() != ReturnRequestStatus.PENDING.getValue()) {
            throw new AppException("Chỉ có thể từ chối yêu cầu đang Chờ xử lý");
        }

        int oldStatus = rr.getStatus();
        rr.setStatus(ReturnRequestStatus.REJECTED.getValue());
        rr = returnRequestRepository.save(rr);

        logHistory(rr, oldStatus, ReturnRequestStatus.REJECTED.getValue(),
                note != null ? note : "Từ chối yêu cầu hoàn trả", staffId);

        // Đổi bill về trạng thái trước (Đã giao) + log OrderStatusHistory
        Bill bill = rr.getBill();
        billService.changeBillStatusAndLogHistory(
                bill.getId(),
                OrderStatus.DELIVERED.getValue(),
                staffId,
                note != null ? note : "Từ chối yêu cầu hoàn trả"
        );

        // Gửi thông báo cho customer
        if (bill.getCustomer() != null) {
            notificationService.sendToCustomer(
                    bill.getCustomer().getId(),
                    "Yêu cầu hoàn trả bị từ chối",
                    "Yêu cầu hoàn trả của đơn " + bill.getId()
                            + " đã bị từ chối. Lý do: "
                            + (note != null ? note : "không đủ điều kiện"),
                    NotificationType.RETURN_REJECTED.getValue(),
                    rr.getId()
            );
        }

        log.info("Từ chối hoàn trả: {} | staff: {}", returnRequestId, staffId);
        return rr;
    }

    // ── Xác nhận hoàn tiền ────────────────────────────────────────────────

    @Override
    @Transactional
    public ReturnRequest confirmRefund(String returnRequestId, String staffId, String note) {
        ReturnRequest rr = findById(returnRequestId);

        if (rr.getStatus() != ReturnRequestStatus.APPROVED.getValue()) {
            throw new AppException("Chỉ xác nhận hoàn tiền khi yêu cầu đã được Duyệt");
        }

        int oldStatus = rr.getStatus();
        rr.setStatus(ReturnRequestStatus.REFUNDED.getValue());
        rr = returnRequestRepository.save(rr);

        logHistory(rr, oldStatus, ReturnRequestStatus.REFUNDED.getValue(),
                note != null ? note : "Đã hoàn tiền cho khách", staffId);

        // Cập nhật BillReturn → trạng thái Đã hoàn tiền
        billReturnRepository.findByReturnRequestId(returnRequestId)
                .ifPresent(br -> {
                    br.setReturnStatus(2); // Đã hoàn tiền

                    // updated_by của BillReturn đang FK tới Account(id) trên DB,
                    // nên phải set bằng accountId tương ứng với staffId.
                    Staff staff = staffRepository.findById(staffId)
                            .orElseThrow(() -> new ResourceNotFoundException("staff", staffId));

                    String accountId = staff.getAccount() != null ? staff.getAccount().getId() : null;
                    br.setUpdatedBy(accountId);

                    billReturnRepository.save(br);
                });

        // Cập nhật payment_status bill → REFUNDED
        // (timeline UI hiện tại dựa trên OrderStatusHistory, nên giữ nguyên bill.status)
        Bill bill = rr.getBill();
        bill.setPaymentStatus(PaymentStatus.REFUNDED.getValue());
        billRepository.save(bill);

        // Nếu bạn muốn timeline thể hiện bước "hoàn tiền", cần thêm mapping sang OrderStatusHistory theo PaymentStatus.
        // Hiện tại chỉ đảm bảo không thiếu timeline do return process.
        log.info("Xác nhận hoàn tiền: {} | staff: {}", returnRequestId, staffId);
        return rr;
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private void logHistory(ReturnRequest rr, Integer oldStatus,
                             int newStatus, String note, String staffId) {
        ReturnStatusHistory history = ReturnStatusHistory.builder()
                .returnRequest(rr)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .note(note)
                .staff(staffId != null ? Staff.builder().id(staffId).build() : null)
                .build();
        returnStatusHistoryRepository.save(history);
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
}