package com.example.skysport1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CounterSaleCustomerResponse {
    private String id;
    private String fullName;
    private String phone;
}