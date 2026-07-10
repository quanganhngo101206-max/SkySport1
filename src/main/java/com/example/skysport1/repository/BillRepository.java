package com.example.skysport1.repository;

import com.example.skysport1.entity.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, String> {

    List<Bill> findByCustomerIdOrderByCreateDateDesc(String customerId);

    List<Bill> findByStatus(Integer status);

    Page<Bill> findByStatus(Integer status, Pageable pageable);

    List<Bill> findByGuestEmail(String guestEmail);

    /**
     * Guest tracking theo contact (email hoặc phone).
     * Bắt buộc customer IS NULL để tránh lộ/tiết lộ đơn của customer đã có tài khoản.
     */
    @Query("SELECT b FROM Bill b " +
            "WHERE b.customer IS NULL " +
            "AND (b.guestEmail = :contact OR b.receiverPhone = :contact) " +
            "ORDER BY b.createDate DESC")
    List<Bill> findGuestBillsByContact(@Param("contact") String contact);

    // Fetch customer cùng lúc để tránh LazyInitializationException
    @Query("SELECT b FROM Bill b LEFT JOIN FETCH b.customer ORDER BY b.createDate DESC")
    List<Bill> findAllWithCustomer();

    @Query("SELECT b FROM Bill b LEFT JOIN FETCH b.customer LEFT JOIN FETCH b.billDetails WHERE b.id = :id")
    Optional<Bill> findByIdWithDetails(String id);

    @Query("SELECT b FROM Bill b LEFT JOIN FETCH b.customer ORDER BY b.createDate DESC")
    List<Bill> findTop5WithCustomer(Pageable pageable);

    // Dashboard: count theo status
    long countByStatus(Integer status);

    /**
     * Dashboard top products:
     * Tránh LazyInitializationException khi AdminDashboardController duyệt b.getBillDetails().
     */
    @Query("""
            SELECT DISTINCT b
            FROM Bill b
            LEFT JOIN FETCH b.billDetails bd
            LEFT JOIN FETCH bd.productDetail pd
            LEFT JOIN FETCH b.customer c
            WHERE b.status = :status
            """)
    List<Bill> findByStatusWithDetails(Integer status);

    // Thêm method này
    @Query("SELECT b FROM Bill b LEFT JOIN FETCH b.customer ORDER BY b.createDate DESC")
    Page<Bill> findAllWithCustomer(Pageable pageable);

    @Query("SELECT b FROM Bill b LEFT JOIN FETCH b.customer WHERE b.status = :status ORDER BY b.createDate DESC")
    Page<Bill> findByStatusWithCustomer(@Param("status") Integer status, Pageable pageable);
}