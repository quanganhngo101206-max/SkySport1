package com.example.skysport1.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO cho admin sửa thông tin khách hàng.
 * Thay cho việc bind thẳng entity Customer qua @ModelAttribute.
 */
@Data
public class CustomerUpdateRequest {

    @NotBlank(message = "Thiếu id khách hàng")
    private String id;

    @NotBlank(message = "Vui lòng nhập họ tên")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(0[3|5|7|8|9])[0-9]{8}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Email(message = "Email không hợp lệ")
    private String email;

    private Boolean gender;

    private LocalDate dob;

    private Integer status;
}