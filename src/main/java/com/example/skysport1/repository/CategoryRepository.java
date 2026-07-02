package com.example.skysport1.repository;

import com.example.skysport1.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    List<Category> findByDeleteFlagFalseOrderByNameAsc();

    List<Category> findByStatusAndDeleteFlagFalse(Integer status);

    boolean existsByName(String name);
}