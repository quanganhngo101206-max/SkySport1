package com.example.skysport1.service;

import com.example.skysport1.entity.Product;
import com.example.skysport1.entity.ProductDetail;
import java.util.List;

public interface ProductService {
    List<Product> findAll();

    List<Product> findAllActive();

    List<Product> findByCategoryId(String categoryId);

    List<Product> findByBrandId(String brandId);

    List<Product> search(String keyword);

    Product findById(String id);

    Product findBySlug(String slug);

    List<ProductDetail> findDetailsByProductId(String productId);

    void save(Product product);

    void update(Product product);

    void delete(String id);
}