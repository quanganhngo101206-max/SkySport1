package com.example.skysport1.repository;

import com.example.skysport1.entity.ImportOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportOrderRepository extends JpaRepository<ImportOrder, String> {
    List<ImportOrder> findByStatus(Integer status);

    List<ImportOrder> findBySupplierId(String supplierId);

    List<ImportOrder> findByStaffId(String staffId);

    @Query("SELECT o FROM ImportOrder o LEFT JOIN FETCH o.supplier")
    List<ImportOrder> findAllWithSupplier();

    @Query("SELECT o FROM ImportOrder o LEFT JOIN FETCH o.supplier WHERE o.status = :status")
    List<ImportOrder> findByStatusWithSupplier(@Param("status") Integer status);
}
