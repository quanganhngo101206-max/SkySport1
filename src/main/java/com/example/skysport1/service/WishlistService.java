package com.example.skysport1.service;

import com.example.skysport1.entity.Customer;
import com.example.skysport1.entity.Product;
import com.example.skysport1.entity.Wishlist;
import com.example.skysport1.entity.WishlistDetail;
import com.example.skysport1.exception.AppException;
import com.example.skysport1.repository.WishlistDetailRepository;
import com.example.skysport1.repository.WishlistRepository;
import com.example.skysport1.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistDetailRepository wishlistDetailRepository;
    private final IdGenerator idGenerator;

    @Transactional
    public Wishlist getOrCreate(String customerId) {
        return wishlistRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Wishlist w = Wishlist.builder()
                            .id(idGenerator.generateWishlistId())
                            .customer(Customer.builder().id(customerId).build())
                            .build();
                    return wishlistRepository.save(w);
                });
    }

    public List<WishlistDetail> findItems(String customerId) {
        Wishlist w = getOrCreate(customerId);
        return wishlistDetailRepository.findByWishlistId(w.getId());
    }

    @Transactional
    public WishlistDetail addProduct(String customerId, String productId) {
        Wishlist w = getOrCreate(customerId);

        boolean exists = wishlistDetailRepository
                .existsByWishlistIdAndProductId(w.getId(), productId);
        if (exists) {
            throw new AppException("Sản phẩm đã có trong danh sách yêu thích");
        }

        WishlistDetail detail = WishlistDetail.builder()
                .wishlist(w)
                .product(Product.builder().id(productId).build())
                .build();
        return wishlistDetailRepository.save(detail);
    }

    @Transactional
    public void removeProduct(String customerId, String productId) {
        Wishlist w = getOrCreate(customerId);
        wishlistDetailRepository.deleteByWishlistIdAndProductId(w.getId(), productId);
    }

    public boolean isInWishlist(String customerId, String productId) {
        return wishlistRepository.findByCustomerId(customerId)
                .map(w -> wishlistDetailRepository.existsByWishlistIdAndProductId(w.getId(), productId))
                .orElse(false);
    }
}
