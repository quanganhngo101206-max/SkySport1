# TODO - SkySport1 (Customer Profile - Shipping Addresses)

- [ ] Bước 1: Mở `CustomerProfileController.java` để xác định cấu trúc hiện tại và cách lấy `customerId` / truyền model.
- [ ] Bước 2: Cập nhật `CustomerProfileController.java`
  - [ ] Nạp `AddressShippingRepository`
  - [ ] GET `/customer/profile`: add `addresses` (danh sách địa chỉ) + `defaultAddressId` (hoặc địa chỉ mặc định)
  - [ ] POST `/customer/profile/address/add`: thêm địa chỉ
  - [ ] POST `/customer/profile/address/update`: sửa địa chỉ
  - [ ] POST `/customer/profile/address/delete`: xóa địa chỉ
  - [ ] POST `/customer/profile/address/set-default`: đặt địa chỉ mặc định
- [ ] Bước 3: Cập nhật `templates/customer/profile/index.html`
  - [ ] Hiển thị danh sách địa chỉ (card/table)
  - [ ] Form thêm địa chỉ
  - [ ] Modal/inline form sửa địa chỉ
  - [ ] Nút “Đặt mặc định”
  - [ ] Nút “Xóa”
  - [ ] Đảm bảo form gửi đúng URL action theo controller
- [ ] Bước 4: Chạy/kiểm tra build hoặc chạy app để đảm bảo không lỗi Thymeleaf và thao tác CRUD hoạt động.