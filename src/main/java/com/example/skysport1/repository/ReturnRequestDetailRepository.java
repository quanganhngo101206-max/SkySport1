package com.example.skysport1.repository;

import com.example.skysport1.entity.ReturnRequestDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestDetailRepository extends JpaRepository<ReturnRequestDetail, Integer> {

    List<ReturnRequestDetail> findByReturnRequestId(String returnRequestId);
}