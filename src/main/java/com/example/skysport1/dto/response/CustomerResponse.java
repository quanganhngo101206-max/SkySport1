package com.example.skysport1.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponse {
    private String id;
    private String fullName;
    private String phone;
    private String email;
    private String genderLabel;      // "Nam", "Nữ"
    private LocalDate dob;
    private Integer status;
    private LocalDateTime createDate;
}
