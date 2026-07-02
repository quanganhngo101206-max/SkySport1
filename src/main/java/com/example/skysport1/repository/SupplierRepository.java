package com.example.skysport1.repository;

import com.example.skysport1.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {

    List<Supplier> findByDeleteFlagFalseOrderByNameAsc();

    List<Supplier> findByStatusAndDeleteFlagFalse(Integer status);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    @Query("SELECT s FROM Supplier s WHERE s.deleteFlag = false AND " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Supplier> searchByName(@Param("keyword") String keyword);
}