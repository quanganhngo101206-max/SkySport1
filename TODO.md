# TODO - Admin bill history (timeline) fix

- [ ] B1: Thêm method công khai trong `BillServiceImpl.java` để (1) cập nhật `Bill.status` và (2) tạo `OrderStatusHistory` tương ứng bằng `logHistory(...)`.
- [ ] B2: Cập nhật `ReturnRequestServiceImpl.java` để khi tạo/approve/reject/confirmRefund cho `ReturnRequest` sẽ gọi method mới trong `BillServiceImpl` thay vì `billRepository.save(bill)` trực tiếp.
- [ ] B3: Build + chạy tests/compile để đảm bảo không lỗi.
