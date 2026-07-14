package com.example.skysport1.repository;

import com.example.skysport1.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Integer> {
    List<PaymentTransaction> findByBillId(String billId);

    Optional<PaymentTransaction> findByTransactionCode(String transactionCode);

    List<PaymentTransaction> findByBillIdAndPaymentStatus(String billId, Integer paymentStatus);

    // Dùng cho cột "Phương thức thanh toán" ở màn hình Audit — mỗi bill có
    // thể có nhiều transaction (VD: thử lại sau khi thất bại), lấy cái mới
    // nhất theo id.
    Optional<PaymentTransaction> findFirstByBillIdOrderByIdDesc(String billId);
}