package com.example.skysport1.repository;

import com.example.skysport1.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByCustomerId(String customerId);
    boolean existsByBillDetailIdAndCustomerId(Integer billDetailId, String customerId);
}
