package com.example.skysport1.repository;

import com.example.skysport1.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Integer> {

    List<Image> findByProductIdOrderByDisplayOrderAsc(String productId);

    Optional<Image> findByProductIdAndIsThumbnailTrue(String productId);

    void deleteByProductId(String productId);
}