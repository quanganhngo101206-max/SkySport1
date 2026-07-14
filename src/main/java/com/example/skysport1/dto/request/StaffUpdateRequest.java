package com.example.skysport1.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO cho admin sửa thông tin nhân viên.
 * Đổi mật khẩu KHÔNG nằm trong DTO này nữa — dùng endpoint riêng
 * /admin/staffs/{id}/change-password để tránh việc admin vô tình đổi
 * mật khẩu người khác khi chỉ định sửa SĐT/email.
 */
@Data
public class StaffUpdateRequest {

    @NotBlank(message = "Thiếu id nhân viên")
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

    private String address;

    private Integer status;

    /**
     * Username tài khoản đăng nhập của nhân viên (chỉ hiển thị, không chỉnh sửa ở màn edit)
     */
    private String username;
}