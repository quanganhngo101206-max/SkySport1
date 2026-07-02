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
}
