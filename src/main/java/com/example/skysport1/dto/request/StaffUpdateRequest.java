package com.example.skysport1.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO cho admin sửa thông tin nhân viên. Mật khẩu để trống nếu không đổi.
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

    // Không @NotBlank vì để trống nghĩa là không đổi mật khẩu
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String password;
}