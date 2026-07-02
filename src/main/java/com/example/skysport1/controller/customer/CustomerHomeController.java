package com.example.skysport1.controller.customer;

import com.example.skysport1.service.ProductService;
import com.example.skysport1.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerHomeController {

    private final ProductService productService;
    private final WishlistService wishlistService;

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        // Lấy sản phẩm nổi bật (có fetch join productDetails)
        var products = productService.findAllActive();
        model.addAttribute("featuredProducts", products.stream().limit(8).toList());
        model.addAttribute("title", "Trang chủ");
        return "customer/home";
    }

    @GetMapping("/detail")
    public String detail(Model model) {
        return "customer/detail";
    }
}