package com.example.skysport1.service.impl;

import com.example.skysport1.entity.Product;
import com.example.skysport1.entity.ProductDetail;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.ProductDetailRepository;
import com.example.skysport1.repository.ProductRepository;
import com.example.skysport1.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductDetailRepository productDetailRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findByDeleteFlag(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAllActive() {
        return productRepository.findAllActiveWithBrandAndCategory();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findByCategoryId(String categoryId) {
        return productRepository.findByCategoryIdWithBrandAndCategory(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findByBrandId(String brandId) {
        return productRepository.findByBrandIdWithBrandAndCategory(brandId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> search(String keyword) {
        return productRepository.searchWithBrandAndCategory(
                keyword == null ? null : keyword.trim()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Product findBySlug(String slug) {
        // ✅ DÙNG METHOD MỚI CÓ FETCH
        return productRepository.findBySlugWithDetails(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + slug));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDetail> findDetailsByProductId(String productId) {
        // ✅ DÙNG METHOD MỚI CÓ FETCH (size, color) — tránh LazyInitializationException
        // khi Thymeleaf truy cập detail.size.name / detail.color.name ngoài transaction
        return productDetailRepository
                .findByProductIdAndStatusAndDeleteFlagWithSizeAndColor(productId, 1, false);
    }

    @Override
    @Transactional
    public void save(Product product) {
        product.setDeleteFlag(false);
        product.setStatus(1);
        product.setCreateDate(LocalDateTime.now());
        product.setUpdateDate(LocalDateTime.now());
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void update(Product product) {
        Product old = findById(product.getId());
        old.setName(product.getName());
        old.setSlug(product.getSlug());
        old.setDescription(product.getDescription());
        old.setGender(product.getGender());
        old.setStatus(product.getStatus());
        old.setBrand(product.getBrand());
        old.setCategory(product.getCategory());
        old.setMaterial(product.getMaterial());
        old.setUpdateDate(LocalDateTime.now());
        old.setUpdatedBy(product.getUpdatedBy());
        productRepository.save(old);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Product product = findById(id);
        product.setDeleteFlag(true);
        product.setUpdateDate(LocalDateTime.now());
        productRepository.save(product);
    }
}