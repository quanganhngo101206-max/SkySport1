package com.example.skysport1.repository;

import com.example.skysport1.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Integer> {
    List<InventoryTransaction> findByProductDetailIdOrderByCreateDateDesc(Integer productDetailId);
    List<InventoryTransaction> findByReferenceId(String referenceId);
    List<InventoryTransaction> findByType(String type);
}
