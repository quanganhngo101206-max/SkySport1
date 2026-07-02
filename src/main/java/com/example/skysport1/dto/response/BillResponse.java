package com.example.skysport1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BillResponse {
    private String id;
    private String customerName;
    private String customerPhone;
    private String guestEmail;
    private String shippingAddress;
    private String receiverName;
    private String receiverPhone;
    private String statusLabel;      // "Chờ xác nhận", "Đã xác nhận", ...
    private String paymentStatusLabel; // "Chưa thanh toán", "Đã thanh toán", ...
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private List<BillDetailResponse> billDetails;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
}
