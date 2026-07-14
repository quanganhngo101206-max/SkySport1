package com.example.skysport1.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO cho admin sửa thông tin khách hàng.
 * Thay cho việc bind thẳng entity Customer qua @ModelAttribute.
 *
 * LƯU Ý: admin/customer/edit.html hiện chỉ cho phép admin đổi "status"
 * (khoá/mở khoá tài khoản) — fullName/phone/email/gender/dob được hiển thị
 * ở dạng disabled nên trình duyệt KHÔNG gửi lên khi submit. Các field đó
 * vẫn giữ lại trong DTO để không phá vỡ chỗ khác lỡ còn tham chiếu, nhưng
 * KHÔNG còn @NotBlank/bắt buộc, và CustomerService.update() cũng không
 * dùng chúng để ghi đè dữ liệu khách hàng.
 */
@Data
public class CustomerUpdateRequest {

    @NotBlank(message = "Thiếu id khách hàng")
    private String id;

    private String fullName;

    @Pattern(regexp = "^(0[3|5|7|8|9])[0-9]{8}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Email(message = "Email không hợp lệ")
    private String email;

    private Boolean gender;

    private LocalDate dob;

    private Integer status;
}