package com.example.skysport1.controller;

import com.example.skysport1.entity.Account;
import com.example.skysport1.entity.Color;
import com.example.skysport1.entity.Product;
import com.example.skysport1.entity.ProductDetail;
import com.example.skysport1.entity.Size;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

//         Danh sách màu/size DUY NHẤT để vẽ swatch/button — trước đây vòng
//         lặp chạy trực tiếp trên productDetails (mỗi dòng = 1 tổ hợp
//         color+size) nên 1 màu có nhiều size sẽ bị vẽ lặp nhiều swatch,
//         và mỗi swatch/button lại gắn cứng với đúng 1 detail.id -> bấm size
//         sau ghi đè mất màu đã chọn trước đó. LinkedHashMap giữ đúng thứ
//         tự xuất hiện trong danh sách gốc.
        List<Color> distinctColors = details.stream()
                .map(ProductDetail::getColor)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Color::getId, c -> c, (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();

        List<Size> distinctSizes = details.stream()
                .map(ProductDetail::getSize)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Size::getId, s -> s, (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();

        model.addAttribute("distinctColors", distinctColors);
        model.addAttribute("distinctSizes", distinctSizes);

        Set<String> wishlistProductIds = wishlistService.findProductIdsInWishlist(getCurrentCustomerId());
        model.addAttribute("inWishlist", wishlistProductIds.contains(product.getId()));
        model.addAttribute("currentUrl", request.getRequestURI());
        return "customer/product/detail";
    }
}