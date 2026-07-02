package com.example.skysport1.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AdminStaffUpdateRequest {

    @NotBlank(message = "ID không được để trống")
    private String id;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9+\\-\\s]{8,15}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Email(message = "Email không hợp lệ")
    private String email;

    @NotNull(message = "Giới tính không được để trống")
    private Boolean gender;

    private LocalDate dob;

    private String address;

    // edit.html dùng select value 1/0
    @NotNull(message = "Trạng thái không được để trống")
    private Integer status;

    // password có thể để trống (không đổi)
    private String password;
}