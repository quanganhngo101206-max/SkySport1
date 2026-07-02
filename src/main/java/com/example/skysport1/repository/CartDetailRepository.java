package com.example.skysport1.repository;

import com.example.skysport1.entity.CartDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartDetailRepository extends JpaRepository<CartDetail, Integer> {
    List<CartDetail> findByCartId(String cartId);

    Optional<CartDetail> findByCartIdAndProductDetailId(String cartId, Integer productDetailId);

    void deleteByCartId(String cartId);

    @Query("SELECT cd FROM CartDetail cd " +
            "LEFT JOIN FETCH cd.productDetail pd " +
            "LEFT JOIN FETCH pd.product p " +
            "LEFT JOIN FETCH pd.size s " +
            "LEFT JOIN FETCH pd.color c " +
            "WHERE cd.cart.id = :cartId")
    List<CartDetail> findByCartIdWithProduct(@Param("cartId") String cartId);
}
