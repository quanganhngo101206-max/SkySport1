package com.example.skysport1.repository;

import com.example.skysport1.entity.CustomerDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerDiscountRepository extends JpaRepository<CustomerDiscount, Integer> {
    boolean existsByCustomerIdAndDiscountCodeId(String customerId, Integer discountCodeId);
    List<CustomerDiscount> findByCustomerId(String customerId);
    Optional<CustomerDiscount> findByCustomerIdAndDiscountCodeId(String customerId, Integer discountCodeId);
}
