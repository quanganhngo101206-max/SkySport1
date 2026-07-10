package com.example.skysport1.config;

import com.example.skysport1.entity.Customer;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.service.CartService;
import com.example.skysport1.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    private static final String SESSION_GUEST_CART = "GUEST_CART";

    private final CartService cartService;
    private final AccountService accountService;
    private final CustomerService customerService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        Collection<SimpleGrantedAuthority> authorities =
                (Collection<SimpleGrantedAuthority>) authentication.getAuthorities();

        boolean isAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isStaff = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));

        if (isAdmin) {
            response.sendRedirect("/admin");
        } else if (isStaff) {
            response.sendRedirect("/staff");
        } else {
            mergeGuestCartIntoCustomerCart(request, authentication);
            response.sendRedirect("/");
        }
    }

    /**
     * Khi khách (chưa đăng nhập) đã thêm sản phẩm vào giỏ hàng (lưu trong
     * session) rồi mới đăng nhập/đăng ký, giỏ hàng session đó cần được gộp
     * vào giỏ hàng DB của customer thay vì bị bỏ quên.
     */
    @SuppressWarnings("unchecked")
    private void mergeGuestCartIntoCustomerCart(HttpServletRequest request, Authentication authentication) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }

        Object raw = session.getAttribute(SESSION_GUEST_CART);
        if (!(raw instanceof Map)) {
            return;
        }

        Map<Integer, Integer> guestCart = (Map<Integer, Integer>) raw;
        if (guestCart.isEmpty()) {
            session.removeAttribute(SESSION_GUEST_CART);
            return;
        }

        try {
            String username = authentication.getName();
            var account = accountService.findByUsername(username);
            Customer customer = customerService.findByAccountId(account.getId());
            String customerId = customer.getId();

            for (Map.Entry<Integer, Integer> entry : guestCart.entrySet()) {
                Integer productDetailId = entry.getKey();
                Integer quantity = entry.getValue();
                if (productDetailId == null || quantity == null || quantity <= 0) {
                    continue;
                }
                try {
                    cartService.addToCart(customerId, productDetailId, quantity);
                } catch (Exception itemEx) {
                    // Sản phẩm hết hàng/không còn tồn tại -> bỏ qua item này,
                    // vẫn tiếp tục merge các item còn lại.
                    log.warn("Bỏ qua item khi merge guest cart: productDetailId={}, err={}",
                            productDetailId, itemEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Không thể merge guest cart vào tài khoản: {}", e.getMessage());
        } finally {
            session.removeAttribute(SESSION_GUEST_CART);
        }
    }
}