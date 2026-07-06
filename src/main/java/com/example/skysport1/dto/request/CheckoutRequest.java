package com.example.skysport1.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {
    private String shippingAddress;
    private String receiverName;
    private String receiverPhone;
    private String paymentId;
    private String discountCode;
    private Integer addressShippingId; // optional
}