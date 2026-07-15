package com.example.skysport1.service;

import com.example.skysport1.entity.Image;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageService {

    List<Image> findByProductId(String productId);

    /**
     * Upload nhiều ảnh cho 1 sản phẩm. Ảnh đầu tiên upload cho sản phẩm chưa
     * có ảnh nào sẽ tự động được đặt làm ảnh đại diện (thumbnail).
     */
    List<Image> uploadImages(String productId, List<MultipartFile> files);

    void deleteImage(Integer imageId);

    /**
     * Đặt 1 ảnh làm đại diện — tự động bỏ cờ thumbnail ở các ảnh khác cùng
     * sản phẩm để luôn chỉ có đúng 1 ảnh đại diện.
     */
    void setThumbnail(Integer imageId);

    /**
     * Lấy URL ảnh đại diện của nhiều sản phẩm cùng lúc — dùng cho trang danh
     * sách sản phẩm, tránh phải query riêng cho từng dòng (N+1).
     * Sản phẩm chưa có ảnh đại diện sẽ không xuất hiện trong map trả về.
     */
    java.util.Map<String, String> findThumbnailUrlsByProductIds(List<String> productIds);
}