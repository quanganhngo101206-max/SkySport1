package com.example.skysport1.repository;

import com.example.skysport1.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SizeRepository extends JpaRepository<Size, String> {

    List<Size> findByDeleteFlagFalse();

    List<Size> findByStatusAndDeleteFlagFalse(Integer status);
}