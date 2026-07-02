package com.example.skysport1.controller;

import com.example.skysport1.entity.Product;
import com.example.skysport1.repository.BrandRepository;
import com.example.skysport1.repository.CategoryRepository;
import com.example.skysport1.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

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
        return "customer/product/list";
    }

    @GetMapping("/san-pham/{slug}")
    public String productDetail(@PathVariable String slug, Model model) {
        var product = productService.findBySlug(slug);
        var details = productService.findDetailsByProductId(product.getId());
        model.addAttribute("product", product);
        model.addAttribute("productDetails", details);
        return "customer/product/detail";
    }
}