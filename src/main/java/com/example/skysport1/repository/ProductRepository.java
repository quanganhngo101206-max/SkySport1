package com.example.skysport1.repository;

import com.example.skysport1.entity.Product;
import com.example.skysport1.entity.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByDeleteFlag(Boolean deleteFlag);

    Optional<Product> findBySlug(String slug);

    // ✅ THÊM METHOD NÀY - Fetch product + brand + category + material + productDetails + size + color
    @Query("""
           SELECT DISTINCT p
           FROM Product p
           LEFT JOIN FETCH p.brand b
           LEFT JOIN FETCH p.category c
           LEFT JOIN FETCH p.material m
           LEFT JOIN FETCH p.productDetails pd
           LEFT JOIN FETCH pd.size s
           LEFT JOIN FETCH pd.color col
           WHERE p.slug = :slug AND p.deleteFlag = false AND p.status = 1
           """)
    Optional<Product> findBySlugWithDetails(@Param("slug") String slug);

    List<Product> findByStatusAndDeleteFlag(Integer status, Boolean deleteFlag);

    List<Product> findByCategoryIdAndStatusAndDeleteFlag(String categoryId, Integer status, Boolean deleteFlag);

    List<Product> findByBrandIdAndStatusAndDeleteFlag(String brandId, Integer status, Boolean deleteFlag);

    @Query("SELECT p FROM Product p WHERE p.deleteFlag = false AND p.status = 1 " +
            "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    List<ProductDetail> findByDeleteFlagFalseAndStatus(Integer status);

    @Query("""
           SELECT DISTINCT p
           FROM Product p
           LEFT JOIN FETCH p.brand b
           LEFT JOIN FETCH p.category c
           LEFT JOIN FETCH p.productDetails pd
           LEFT JOIN FETCH pd.size s
           LEFT JOIN FETCH pd.color col
           WHERE p.status = 1 AND p.deleteFlag = false
           """)
    List<Product> findAllActiveWithBrandAndCategory();

    @Query("""
           SELECT DISTINCT p
           FROM Product p
           LEFT JOIN FETCH p.brand b
           LEFT JOIN FETCH p.category c
           LEFT JOIN FETCH p.productDetails pd
           LEFT JOIN FETCH pd.size s
           LEFT JOIN FETCH pd.color col
           WHERE p.status = 1 AND p.deleteFlag = false
             AND p.brand.id = :brandId
           """)
    List<Product> findByBrandIdWithBrandAndCategory(@Param("brandId") String brandId);

    @Query("""
           SELECT DISTINCT p
           FROM Product p
           LEFT JOIN FETCH p.brand b
           LEFT JOIN FETCH p.category c
           LEFT JOIN FETCH p.productDetails pd
           LEFT JOIN FETCH pd.size s
           LEFT JOIN FETCH pd.color col
           WHERE p.status = 1 AND p.deleteFlag = false
             AND p.category.id = :categoryId
           """)
    List<Product> findByCategoryIdWithBrandAndCategory(@Param("categoryId") String categoryId);

    @Query("""
           SELECT DISTINCT p
           FROM Product p
           LEFT JOIN FETCH p.brand b
           LEFT JOIN FETCH p.category c
           LEFT JOIN FETCH p.productDetails pd
           LEFT JOIN FETCH pd.size s
           LEFT JOIN FETCH pd.color col
           WHERE p.status = 1 AND p.deleteFlag = false
             AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
           """)
    List<Product> searchWithBrandAndCategory(@Param("keyword") String keyword);
}