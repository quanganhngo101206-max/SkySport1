package com.example.skysport1.repository;

import com.example.skysport1.entity.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductDetailRepository extends JpaRepository<ProductDetail, Integer> {
    List<ProductDetail> findByProductId(String productId);

    List<ProductDetail> findByProductIdAndStatusAndDeleteFlag(String productId, Integer status, Boolean deleteFlag);

    Optional<ProductDetail> findBySku(String sku);

    Optional<ProductDetail> findByBarcode(String barcode);

    Optional<ProductDetail> findByProductIdAndSizeIdAndColorId(String productId, String sizeId, String colorId);

    List<ProductDetail> findByProductIdAndDeleteFlagFalse(String productId);

    // Dùng cho trang "Biến thể sản phẩm" (admin): nạp sẵn size + color
    // để tránh LazyInitializationException khi Thymeleaf render ngoài transaction.
    @Query("SELECT pd FROM ProductDetail pd " +
            "LEFT JOIN FETCH pd.size " +
            "LEFT JOIN FETCH pd.color " +
            "WHERE pd.product.id = :productId " +
            "AND pd.deleteFlag = false")
    List<ProductDetail> findByProductIdAndDeleteFlagFalseWithSizeAndColor(@Param("productId") String productId);

    // Dùng cho dropdown chọn biến thể khi tạo phiếu nhập kho (admin)
    @Query("SELECT pd FROM ProductDetail pd " +
            "LEFT JOIN FETCH pd.product " +
            "LEFT JOIN FETCH pd.size " +
            "LEFT JOIN FETCH pd.color " +
            "WHERE pd.deleteFlag = false " +
            "ORDER BY pd.product.name ASC")
    List<ProductDetail> findAllActiveWithProductSizeColor();

    // Dùng cho trang chi tiết sản phẩm (customer): nạp sẵn size + color
    // để tránh LazyInitializationException khi Thymeleaf render ngoài transaction.
    @Query("SELECT pd FROM ProductDetail pd " +
            "LEFT JOIN FETCH pd.size " +
            "LEFT JOIN FETCH pd.color " +
            "WHERE pd.product.id = :productId " +
            "AND pd.status = :status " +
            "AND pd.deleteFlag = :deleteFlag")
    List<ProductDetail> findByProductIdAndStatusAndDeleteFlagWithSizeAndColor(
            @Param("productId") String productId,
            @Param("status") Integer status,
            @Param("deleteFlag") Boolean deleteFlag);

    // Dùng cho guest cart: load ProductDetail kèm product + size + color
    @Query("SELECT pd FROM ProductDetail pd " +
            "LEFT JOIN FETCH pd.product " +
            "LEFT JOIN FETCH pd.size " +
            "LEFT JOIN FETCH pd.color " +
            "WHERE pd.id = :id")
    Optional<ProductDetail> findByIdWithProductSizeColor(@Param("id") Integer id);

    // Dùng cho màn "Bán hàng tại quầy": tìm theo SKU hoặc tên sản phẩm
    @Query("SELECT pd FROM ProductDetail pd " +
            "LEFT JOIN FETCH pd.product p " +
            "LEFT JOIN FETCH pd.size " +
            "LEFT JOIN FETCH pd.color " +
            "WHERE pd.deleteFlag = false AND pd.status = 1 " +
            "AND (lower(pd.sku) LIKE lower(concat('%', :keyword, '%')) " +
            "     OR lower(p.name) LIKE lower(concat('%', :keyword, '%')))")
    List<ProductDetail> searchForCounterSale(@Param("keyword") String keyword);
}