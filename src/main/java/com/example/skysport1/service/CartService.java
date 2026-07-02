package com.example.skysport1.service;

import com.example.skysport1.entity.Cart;
import com.example.skysport1.entity.CartDetail;
import com.example.skysport1.entity.Customer;
import com.example.skysport1.entity.ProductDetail;
import com.example.skysport1.exception.AppException;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.CartDetailRepository;
import com.example.skysport1.repository.CartRepository;
import com.example.skysport1.repository.ProductDetailRepository;
import com.example.skysport1.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartDetailRepository cartDetailRepository;
    private final ProductDetailRepository productDetailRepository;
    private final IdGenerator idGenerator;

    /**
     * Lấy hoặc tạo mới cart cho customer
     */
    @Transactional
    public Cart getOrCreateCart(String customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .id(idGenerator.generateCartId())
                            .customer(Customer.builder().id(customerId).build())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    public List<CartDetail> getCartDetails(String customerId) {
        Cart cart = getOrCreateCart(customerId);
        return cartDetailRepository.findByCartId(cart.getId());
    }

    @Transactional
    public CartDetail addToCart(String customerId, Integer productDetailId, int quantity) {
        // Validate sản phẩm
        ProductDetail pd = productDetailRepository.findById(productDetailId)
                .orElseThrow(() -> new ResourceNotFoundException("sản phẩm", String.valueOf(productDetailId)));

        if (pd.getStatus() != 1 || Boolean.TRUE.equals(pd.getDeleteFlag())) {
            throw new AppException("Sản phẩm không còn bán");
        }
        if (pd.getQuantity() < quantity) {
            throw new AppException("Số lượng tồn kho không đủ (còn " + pd.getQuantity() + ")");
        }

        Cart cart = getOrCreateCart(customerId);

        // Nếu đã có trong giỏ thì cộng thêm
        return cartDetailRepository
                .findByCartIdAndProductDetailId(cart.getId(), productDetailId)
                .map(existing -> {
                    int newQty = existing.getQuantity() + quantity;
                    if (pd.getQuantity() < newQty) {
                        throw new AppException("Số lượng tồn kho không đủ (còn " + pd.getQuantity() + ")");
                    }
                    existing.setQuantity(newQty);
                    return cartDetailRepository.save(existing);
                })
                .orElseGet(() -> {
                    CartDetail detail = CartDetail.builder()
                            .cart(cart)
                            .productDetail(pd)
                            .quantity(quantity)
                            .build();
                    return cartDetailRepository.save(detail);
                });
    }

    @Transactional
    public CartDetail updateQuantity(String customerId, Integer cartDetailId, int quantity) {
        CartDetail detail = cartDetailRepository.findById(cartDetailId)
                .orElseThrow(() -> new ResourceNotFoundException("chi tiết giỏ hàng", String.valueOf(cartDetailId)));

        // Kiểm tra detail thuộc cart của customer
        Cart cart = getOrCreateCart(customerId);
        if (!detail.getCart().getId().equals(cart.getId())) {
            throw new AppException("Không có quyền chỉnh sửa");
        }

        if (quantity <= 0) {
            cartDetailRepository.delete(detail);
            return null;
        }

        int stock = detail.getProductDetail().getQuantity();
        if (stock < quantity) {
            throw new AppException("Số lượng tồn kho không đủ (còn " + stock + ")");
        }
        detail.setQuantity(quantity);
        return cartDetailRepository.save(detail);
    }

    @Transactional
    public void removeItem(String customerId, Integer cartDetailId) {
        CartDetail detail = cartDetailRepository.findById(cartDetailId)
                .orElseThrow(() -> new ResourceNotFoundException("chi tiết giỏ hàng", String.valueOf(cartDetailId)));
        Cart cart = getOrCreateCart(customerId);
        if (!detail.getCart().getId().equals(cart.getId())) {
            throw new AppException("Không có quyền xóa");
        }
        cartDetailRepository.delete(detail);
    }

    @Transactional
    public void clearCart(String customerId) {
        Cart cart = getOrCreateCart(customerId);
        cartDetailRepository.deleteByCartId(cart.getId());
    }

    public int countItems(String customerId) {
        Cart cart = cartRepository.findByCustomerId(customerId).orElse(null);
        if (cart == null) return 0;
        return cartDetailRepository.findByCartId(cart.getId())
                .stream().mapToInt(CartDetail::getQuantity).sum();
    }
}
