package com.example.skysport1.repository;

import com.example.skysport1.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Integer> {

    // Stable order for history timeline:
    // SQL Server IDENTITY(id) is never NULL, never duplicated and always increases in insert order.
    // createDate can be NULL / backfilled / duplicated -> not reliable for ordering.
    List<OrderStatusHistory> findByBillIdOrderByIdAsc(String billId);
}
