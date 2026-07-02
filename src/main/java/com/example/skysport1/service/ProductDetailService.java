package com.example.skysport1.service;

import com.example.skysport1.entity.ProductDetail;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.ProductDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductDetailService {

    private final ProductDetailRepository productDetailRepository;

    public List<ProductDetail> findByProductId(String productId) {
        return productDetailRepository
                .findByProductIdAndDeleteFlagFalse(productId);
    }

    // Dùng cho dropdown chọn biến thể khi tạo phiếu nhập kho (admin)
    public List<ProductDetail> findAllActive() {
        return productDetailRepository.findAllActiveWithProductSizeColor();
    }

    public ProductDetail findById(Integer id) {
        return productDetailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Biến thể sản phẩm", String.valueOf(id)));
    }

    @Transactional
    public void save(ProductDetail detail) {
        productDetailRepository.save(detail);
    }

    @Transactional
    public void update(ProductDetail detail) {
        ProductDetail old = findById(detail.getId());
        old.setSize(detail.getSize());
        old.setColor(detail.getColor());
        old.setPrice(detail.getPrice());
        old.setCostPrice(detail.getCostPrice());
        old.setQuantity(detail.getQuantity());
        old.setSku(detail.getSku());
        old.setBarcode(detail.getBarcode());
        old.setStatus(detail.getStatus());
        old.setUpdateDate(LocalDateTime.now());
        old.setUpdatedBy(detail.getUpdatedBy());
        productDetailRepository.save(old);
    }

    @Transactional
    public void delete(Integer id) {
        ProductDetail detail = findById(id);
        detail.setDeleteFlag(true);
        detail.setUpdateDate(LocalDateTime.now());
        productDetailRepository.save(detail);
    }

    // Cộng/trừ tồn kho — dùng khi bán hàng hoặc nhập kho
    @Transactional
    public void adjustQuantity(Integer id, int change) {
        ProductDetail detail = findById(id);
        int newQty = (detail.getQuantity() != null ? detail.getQuantity() : 0) + change;
        if (newQty < 0) throw new IllegalStateException("Tồn kho không đủ!");
        detail.setQuantity(newQty);
        detail.setUpdateDate(LocalDateTime.now());
        productDetailRepository.save(detail);
    }
}