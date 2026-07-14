package com.example.skysport1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO riêng cho admin đổi mật khẩu nhân viên — tách khỏi form sửa thông tin
 * để tránh việc sửa nhầm mật khẩu người khác khi chỉ định sửa SĐT/email.
 */
@Data
public class StaffChangePasswordRequest {

    @NotBlank(message = "Thiếu id nhân viên")
    private String id;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String newPassword;
}