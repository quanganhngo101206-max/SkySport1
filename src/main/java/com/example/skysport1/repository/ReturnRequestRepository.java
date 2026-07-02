package com.example.skysport1.repository;

import com.example.skysport1.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, String> {

    List<ReturnRequest> findByBillId(String billId);

    List<ReturnRequest> findByStatus(Integer status);

    List<ReturnRequest> findByBillIdOrderByCreateDateDesc(String billId);
}