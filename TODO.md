# TODO - SkySport1

- [ ] UI: Thêm nút 🗑️ “Hủy sản phẩm này” theo từng dòng BillDetail
  - [ ] Sửa `src/main/resources/templates/customer/order/detail.html`
  - [ ] Sửa `src/main/resources/templates/guest/order-detail.html`
- [ ] (Nếu cần) kiểm tra hiển thị/disable theo `bill.status` và `billDetail.itemStatus`:
  - `itemStatus == 1` (NORMAL) → hiển thị nút hủy
  - `bill.status in {1,2}` (PENDING/CONFIRMED) → cho phép thao tác
- [ ] Smoke test: bấm nút hủy từng dòng, xác nhận route đúng (customer/guest) và CSRF hoạt động
