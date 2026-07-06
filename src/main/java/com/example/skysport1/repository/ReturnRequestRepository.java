package com.example.skysport1.repository;

import com.example.skysport1.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, String> {

    @Query("""
           select r
           from ReturnRequest r
           left join fetch r.bill b
           left join fetch b.customer c
           where (r.bill.id = :billId)
           order by r.createDate desc
           """)
    List<ReturnRequest> findByBillIdOrderByCreateDateDesc(@Param("billId") String billId);

    @Query("""
           select r
           from ReturnRequest r
           left join fetch r.bill b
           left join fetch b.customer c
           where (r.bill.id = :billId)
           """)
    List<ReturnRequest> findByBillId(@Param("billId") String billId);

    @Query("""
           select r
           from ReturnRequest r
           left join fetch r.bill b
           left join fetch b.customer c
           order by r.createDate desc
           """)
    List<ReturnRequest> findAllWithBillAndCustomer();

    @Query("""
           select r
           from ReturnRequest r
           left join fetch r.bill b
           left join fetch b.customer c
           where (r.status = :status)
           order by r.createDate desc
           """)
    List<ReturnRequest> findByStatus(@Param("status") Integer status);

    @Query("""
           select r
           from ReturnRequest r
           left join fetch r.bill b
           left join fetch b.customer c
           where r.id = :id
           """)
    java.util.Optional<ReturnRequest> findByIdWithBillAndCustomer(@Param("id") String id);
}