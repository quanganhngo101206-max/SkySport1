package com.example.skysport1.repository;

import com.example.skysport1.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, String> {
    Optional<Wishlist> findByCustomerId(String customerId);
}
