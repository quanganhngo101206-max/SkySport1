package com.example.skysport1.repository;

import com.example.skysport1.entity.WishlistDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishlistDetailRepository extends JpaRepository<WishlistDetail, Integer> {

    List<WishlistDetail> findByWishlistId(String wishlistId);

    boolean existsByWishlistIdAndProductId(String wishlistId, String productId);

    void deleteByWishlistIdAndProductId(String wishlistId, String productId);
}