package com.example.skysport1.repository;

import com.example.skysport1.entity.BillDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillDetailRepository extends JpaRepository<BillDetail, Integer> {
    List<BillDetail> findByBillId(String billId);
    Optional<BillDetail> findByBillIdAndProductDetailId(String billId, Integer productDetailId);
}