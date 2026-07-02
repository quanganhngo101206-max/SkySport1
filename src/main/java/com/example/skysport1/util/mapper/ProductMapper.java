package com.example.skysport1.util.mapper;

import com.example.skysport1.dto.response.*;
import com.example.skysport1.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) return null;

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .genderLabel(getGenderLabel(product.getGender()))
                .statusLabel(getStatusLabel(product.getStatus()))
                .brand(toBrandResponse(product.getBrand()))
                .category(toCategoryResponse(product.getCategory()))
                .materialName(product.getMaterial() != null ? product.getMaterial().getName() : null)
                .productDetails(toDetailResponses(product.getProductDetails()))
                .createDate(product.getCreateDate())
                .build();
    }

    public List<ProductResponse> toResponses(List<Product> products) {
        if (products == null) return List.of();
        return products.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private BrandResponse toBrandResponse(Brand brand) {
        if (brand == null) return null;
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .description(brand.getDescription())
                .status(brand.getStatus())
                .build();
    }

    private CategoryResponse toCategoryResponse(Category category) {
        if (category == null) return null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .status(category.getStatus())
                .build();
    }

    private List<ProductDetailResponse> toDetailResponses(List<ProductDetail> details) {
        if (details == null) return List.of();
        return details.stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    private ProductDetailResponse toDetailResponse(ProductDetail detail) {
        if (detail == null) return null;
        return ProductDetailResponse.builder()
                .id(detail.getId())
                .sizeName(detail.getSize() != null ? detail.getSize().getName() : null)
                .colorName(detail.getColor() != null ? detail.getColor().getName() : null)
                .colorHexCode(detail.getColor() != null ? detail.getColor().getHexCode() : null)
                .sku(detail.getSku())
                .price(detail.getPrice())
                .quantity(detail.getQuantity())
                .status(detail.getStatus())
                .build();
    }

    private String getGenderLabel(Integer gender) {
        if (gender == null) return "Unisex";
        return switch (gender) {
            case 1 -> "Nam";
            case 2 -> "Nữ";
            default -> "Unisex";
        };
    }

    private String getStatusLabel(Integer status) {
        if (status == null) return "Không xác định";
        return switch (status) {
            case 0 -> "Ngừng bán";
            case 1 -> "Đang bán";
            case 2 -> "Hết hàng";
            default -> "Không xác định";
        };
    }
}