package com.example.skysport1.repository;

import com.example.skysport1.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColorRepository extends JpaRepository<Color, String> {

    List<Color> findByDeleteFlagFalse();

    List<Color> findByStatusAndDeleteFlagFalse(Integer status);
}