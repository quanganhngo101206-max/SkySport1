# TODO - Hoàn thiện Admin (SkySport1)

## 1) Admin Bill
- [ ] Rà lại `AdminBillController` + template `templates/admin/bill/*`
- [ ] Đảm bảo có đủ nút/action cập nhật trạng thái thanh toán/đơn hàng
- [ ] Kiểm tra history: `OrderStatusHistory`, `PaymentTransaction` được ghi đúng khi đổi trạng thái
- [ ] UI hiển thị subtotal/shipping/discount/total khớp với `BillServiceImpl`

## 2) Admin Import Order
- [ ] Rà `AdminImportOrderController` + template `templates/admin/import-order/*`
- [ ] Khi duyệt import: cộng tồn đúng vào `ProductDetail.quantity`
- [ ] Log `InventoryTransaction` đúng: type/quantityChange/before/after
- [ ] History `ImportStatusHistory` được tạo đúng trạng thái
- [ ] Validate input (quantity > 0, import detail rỗng, trùng productDetail…)

## 3) Admin Return Request
- [ ] Rà `AdminReturnRequestController` + template `templates/admin/return-request/*`
- [ ] `approve/reject/confirmRefund` cập nhật:
  - [ ] trạng thái RR (`ReturnRequestStatus`)
  - [ ] hoàn tiền (`PaymentStatus` / `BillReturn`)
  - [ ] tồn kho khi approve (log InventoryTransaction + cập nhật billDetail.returnQuantity)
- [ ] Kiểm tra nghiệp vụ “tổng tiền hoàn” có tính đúng theo voucher/discount (nếu nghiệp vụ yêu cầu)

## 4) Admin Discount Code
- [ ] Rà `AdminDiscountCodeController` + template `templates/admin/discount-code/*`
- [ ] Kiểm tra validate voucher trong `DiscountCodeService.validate(...)`
- [ ] Kiểm tra giới hạn `quantity`, `usedCount`, `deleteFlag`, `status` khi apply
- [ ] Khi tạo bill: `recordUsage(...)` tăng usedCount + tạo `CustomerDiscount` đúng

## 5) Admin UI common (filter/search/pagination)
- [ ] Rà các template list (bill/import/return/discount/product/supplier/notification/profile)
- [ ] Kiểm tra binding params (search keyword, status, date range…)
- [ ] Đảm bảo phân trang đúng `pageResult`, `totalPages`, preserving query params

## 6) Testing/Run
- [ ] Chạy `mvnw.cmd test` hoặc ít nhất `mvnw.cmd -q -DskipTests compile` (nếu môi trường JDK đã đúng)
- [ ] Nếu compile fail do JAVA_HOME/JDK: hướng dẫn lại cách cấu hình để test được