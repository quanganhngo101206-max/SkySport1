package com.example.skysport1.repository;

import com.example.skysport1.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByAccountId(String accountId);
    boolean existsByPhone(String phone);

    // Sử dụng native query với SELECT TOP cho SQL Server
    @Query(value = "SELECT * FROM customer ORDER BY create_date DESC OFFSET 0 ROWS FETCH NEXT ?1 ROWS ONLY", nativeQuery = true)
    List<Customer> findTopRecent(int limit);

    long countByCreateDateBetween(LocalDateTime start, LocalDateTime end);

    List<Customer> findByDeleteFlagFalseAndStatus(Integer status);
}