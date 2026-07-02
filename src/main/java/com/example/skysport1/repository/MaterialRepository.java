package com.example.skysport1.repository;

import com.example.skysport1.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, String> {

    List<Material> findByDeleteFlagFalseOrderByNameAsc();

    List<Material> findByStatusAndDeleteFlagFalse(Integer status);
}