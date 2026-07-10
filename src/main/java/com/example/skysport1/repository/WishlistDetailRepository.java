package com.example.skysport1.repository;

import com.example.skysport1.entity.WishlistDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishlistDetailRepository extends JpaRepository<WishlistDetail, Integer> {

    List<WishlistDetail> findByWishlistId(String wishlistId);

    // Dùng cho trang danh sách yêu thích: fetch sẵn product và
    // product.productDetails trong 1 lần query. Bắt buộc vì
    // spring.jpa.open-in-view=false -> template không còn Session để lazy-load
    // khi render (xem LazyInitializationException khi truy cập
    // item.product.productDetails).
    @Query("SELECT DISTINCT wd FROM WishlistDetail wd " +
            "JOIN FETCH wd.product p " +
            "LEFT JOIN FETCH p.productDetails " +
            "WHERE wd.wishlist.id = :wishlistId")
    List<WishlistDetail> findByWishlistIdWithProduct(@Param("wishlistId") String wishlistId);

    boolean existsByWishlistIdAndProductId(String wishlistId, String productId);

    void deleteByWishlistIdAndProductId(String wishlistId, String productId);

    // Dùng để đánh dấu nút tim đã chọn/chưa chọn cho cả 1 danh sách sản phẩm
    // trong 1 lần query, tránh N+1 khi render trang danh sách.
    @Query("SELECT wd.product.id FROM WishlistDetail wd WHERE wd.wishlist.id = :wishlistId")
    List<String> findProductIdsByWishlistId(@Param("wishlistId") String wishlistId);
}