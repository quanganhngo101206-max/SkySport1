package com.example.skysport1.repository;

import com.example.skysport1.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Integer> {

    // Sort primary by createDate, secondary by id to keep stable ordering
    List<OrderStatusHistory> findByBillIdOrderByCreateDateAsc(String billId);

    // Secondary sort to avoid “missing/incorrect ordering” feeling when multiple rows have same/null createDate
    List<OrderStatusHistory> findByBillIdOrderByCreateDateAscIdAsc(String billId);
}
