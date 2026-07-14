package com.example.skysport1.repository;

import com.example.skysport1.entity.ReturnStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnStatusHistoryRepository extends JpaRepository<ReturnStatusHistory, Integer> {

    // Dùng để hiển thị dòng thời gian xử lý ở trang chi tiết hoàn trả (mục 15)
    List<ReturnStatusHistory> findByReturnRequestIdOrderByIdAsc(String returnRequestId);
}