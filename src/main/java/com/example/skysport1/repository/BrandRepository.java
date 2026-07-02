package com.example.skysport1.repository;

import com.example.skysport1.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, String> {

    // Sửa: bỏ tham số Boolean thừa — Spring Data tự hiểu false từ tên method
    List<Brand> findByDeleteFlagFalseOrderByNameAsc();

    List<Brand> findByStatusAndDeleteFlagFalse(Integer status);

    boolean existsByName(String name);
}