package com.example.skysport1.repository;

import com.example.skysport1.entity.BillReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillReturnRepository extends JpaRepository<BillReturn, String> {

    List<BillReturn> findByBillId(String billId);

    Optional<BillReturn> findByReturnRequestId(String returnRequestId);

    List<BillReturn> findByReturnStatus(Integer returnStatus);
}