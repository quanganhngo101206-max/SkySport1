package com.example.skysport1.controller.customer;

import com.example.skysport1.entity.Account;
import com.example.skysport1.entity.WishlistDetail;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.service.CustomerService;
import com.example.skysport1.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customer/wishlist")
@RequiredArgsConstructor
@Slf4j
public class CustomerWishlistController {

    private final WishlistService wishlistService;
    private final CustomerService customerService;
    private final AccountService accountService;

    /**
     * Danh sách yêu thích
     */
    @GetMapping
    public String list(Authentication auth, Model model) {
        String customerId = getCurrentCustomerId(auth);
        List<WishlistDetail> items = wishlistService.findItems(customerId);

        model.addAttribute("items", items);
        model.addAttribute("title", "Sản phẩm yêu thích");
        model.addAttribute("pageContent", "customer/wishlist/index");
        return "layouts/customer/layout";
    }

    /**
     * Thêm vào yêu thích
     */
    @PostMapping("/add")
    public String add(@RequestParam String productId, Authentication auth, RedirectAttributes ra) {
        try {
            String customerId = getCurrentCustomerId(auth);
            wishlistService.addProduct(customerId, productId);
            ra.addFlashAttribute("success", "Đã thêm vào danh sách yêu thích");
        } catch (Exception e) {
            log.error("Error adding to wishlist: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/wishlist";
    }

    /**
     * Xóa khỏi yêu thích
     */
    @PostMapping("/remove")
    public String remove(@RequestParam String productId, Authentication auth, RedirectAttributes ra) {
        try {
            String customerId = getCurrentCustomerId(auth);
            wishlistService.removeProduct(customerId, productId);
            ra.addFlashAttribute("success", "Đã xóa khỏi danh sách yêu thích");
        } catch (Exception e) {
            log.error("Error removing from wishlist: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/wishlist";
    }

    /**
     * AJAX: Kiểm tra trong wishlist (cho product detail page)
     */
    @GetMapping("/check/{productId}")
    @ResponseBody
    public boolean check(@PathVariable String productId, Authentication auth) {
        try {
            String customerId = getCurrentCustomerId(auth);
            return wishlistService.isInWishlist(customerId, productId);
        } catch (Exception e) {
            return false;
        }
    }

    private String getCurrentCustomerId(Authentication auth) {
        String username = auth.getName();
        Account account = accountService.findByUsername(username);
        return customerService.findByAccountId(account.getId()).getId();
    }
}