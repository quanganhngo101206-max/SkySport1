package com.example.skysport1.repository;

import com.example.skysport1.entity.ReturnStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReturnStatusHistoryRepository extends JpaRepository<ReturnStatusHistory, Integer> {
}
