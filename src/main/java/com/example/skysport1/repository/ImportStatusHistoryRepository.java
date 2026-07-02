package com.example.skysport1.repository;

import com.example.skysport1.entity.ImportStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportStatusHistoryRepository extends JpaRepository<ImportStatusHistory, Integer> {
}
