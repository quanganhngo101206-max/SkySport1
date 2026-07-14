package com.example.skysport1.repository;

import com.example.skysport1.entity.CustomerDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerDiscountRepository extends JpaRepository<CustomerDiscount, Integer> {
    boolean existsByCustomerIdAndDiscountCodeId(String customerId, Integer discountCodeId);
    // Đếm số lần khách đã dùng mã này — dùng để so với maxUsagePerCustomer
    // (thay cho existsBy cũ, vì giờ 1 khách có thể dùng nhiều lần).
    long countByCustomerIdAndDiscountCodeId(String customerId, Integer discountCodeId);
    List<CustomerDiscount> findByCustomerId(String customerId);
    Optional<CustomerDiscount> findByCustomerIdAndDiscountCodeId(String customerId, Integer discountCodeId);

    // Dùng ở trang chi tiết mã giảm giá (admin) để xem những khách hàng nào
    // đã dùng mã này, dùng lúc nào. Phải LEFT JOIN FETCH customer vì
    // spring.jpa.open-in-view=false — template truy cập u.customer.fullName
    // sau khi transaction đã đóng, không fetch sẵn sẽ vỡ LazyInitializationException.
    @Query("SELECT cd FROM CustomerDiscount cd LEFT JOIN FETCH cd.customer WHERE cd.discountCode.id = :discountCodeId")
    List<CustomerDiscount> findByDiscountCodeId(@Param("discountCodeId") Integer discountCodeId);
}