package com.example.skysport1.controller;

import com.example.skysport1.entity.Account;
import com.example.skysport1.entity.Product;
import com.example.skysport1.repository.BrandRepository;
import com.example.skysport1.repository.CategoryRepository;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.service.CustomerService;
import com.example.skysport1.service.ProductService;
import com.example.skysport1.service.WishlistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final WishlistService wishlistService;
    private final AccountService accountService;
    private final CustomerService customerService;

    /**
     * customerId của người đang đăng nhập (customer), null nếu là guest
     * hoặc chưa đăng nhập. Dùng để hiển thị trạng thái nút tim wishlist.
     */
    private String getCurrentCustomerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        try {
            Account account = accountService.findByUsername(auth.getName());
            return customerService.findByAccountId(account.getId()).getId();
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        // ✅ Dùng method đã fetch brand + category + productDetails
        List<Product> products = productService.findAllActive();
        // Method này đã dùng fetch join để load brand, category, productDetails

        model.addAttribute("featuredProducts", products.stream().limit(8).toList());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("brands", brandRepository.findAll());
        return "home";
    }

    @GetMapping("/san-pham")
    public String productList(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String categoryId,
                              @RequestParam(required = false) String brandId,
                              HttpServletRequest request,
                              Model model) {
        List<Product> products;
        if (keyword != null && !keyword.isBlank()) {
            products = productService.search(keyword);
        } else if (categoryId != null) {
            products = productService.findByCategoryId(categoryId);
        } else if (brandId != null) {
            products = productService.findByBrandId(brandId);
        } else {
            products = productService.findAllActive();
        }
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("brands", brandRepository.findAll());
        model.addAttribute("wishlistProductIds", wishlistService.findProductIdsInWishlist(getCurrentCustomerId()));
        String currentUrl = request.getRequestURI()
                + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        model.addAttribute("currentUrl", currentUrl);
        return "customer/product/list";
    }

    @GetMapping("/san-pham/{slug}")
    public String productDetail(@PathVariable String slug, HttpServletRequest request, Model model) {
        var product = productService.findBySlug(slug);
        var details = productService.findDetailsByProductId(product.getId());
        model.addAttribute("product", product);
        model.addAttribute("productDetails", details);
        Set<String> wishlistProductIds = wishlistService.findProductIdsInWishlist(getCurrentCustomerId());
        model.addAttribute("inWishlist", wishlistProductIds.contains(product.getId()));
        model.addAttribute("currentUrl", request.getRequestURI());
        return "customer/product/detail";
    }
}