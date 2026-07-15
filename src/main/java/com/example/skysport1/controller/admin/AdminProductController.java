package com.example.skysport1.controller.admin;

import com.example.skysport1.entity.*;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.service.*;
import com.example.skysport1.util.IdGenerator;
import com.example.skysport1.util.SlugUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@Transactional
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final ProductService    productService;
    private final ProductDetailService productDetailService;
    private final BrandService      brandService;
    private final CategoryService   categoryService;
    private final MaterialService   materialService;
    private final SizeService       sizeService;
    private final ColorService      colorService;
    private final IdGenerator       idGenerator;
    private final ImageService      imageService;

    // ============================================================
    // PRODUCT LIST
    // ============================================================

    @GetMapping
    public String redirectToList() {
        return "redirect:/admin/products/list";
    }

    @GetMapping("/list")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String brandId,
                       @RequestParam(required = false) String categoryId,
                       @RequestParam(required = false) Integer status,
                       Model model) {
        List<Product> products;
        if (keyword != null && !keyword.isBlank()) {
            products = productService.search(keyword);
        } else if (brandId != null && !brandId.isBlank()) {
            products = productService.findByBrandId(brandId);
        } else if (categoryId != null && !categoryId.isBlank()) {
            products = productService.findByCategoryId(categoryId);
        } else {
            products = productService.findAllActive();
        }

        model.addAttribute("products",   products);
        model.addAttribute("thumbnails", imageService.findThumbnailUrlsByProductIds(
                products.stream().map(Product::getId).toList()));
        model.addAttribute("brands",     brandService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("keyword",    keyword);
        model.addAttribute("brandId",    brandId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("title", "Danh sách sản phẩm");
        model.addAttribute("pageContent", "admin/product/list");
        return "layouts/adminlte/layout";
    }

    // ============================================================
    // CREATE PRODUCT
    // ============================================================

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("product",    new Product());
        model.addAttribute("brands",     brandService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("materials",  materialService.findAll());
        model.addAttribute("pageContent", "admin/product/create");
        model.addAttribute("title", "Thêm sản phẩm");
        return "layouts/adminlte/layout";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Product product,
                       BindingResult bindingResult,
                       Authentication auth,
                       Model model,  // ✅ Thêm Model vào parameter
                       RedirectAttributes ra) {

        // ✅ Kiểm tra validation trước
        if (bindingResult.hasErrors()) {
            // Load lại dữ liệu cần thiết cho form
            model.addAttribute("product", product);  // Giữ lại data đã nhập
            model.addAttribute("brands", brandService.findAll());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("materials", materialService.findAll());
            model.addAttribute("pageContent", "admin/product/create");
            model.addAttribute("title", "Thêm sản phẩm");

            // Thêm lỗi chi tiết để hiển thị
            bindingResult.getAllErrors().forEach(error ->
                    log.warn("Validation error: {}", error.getDefaultMessage())
            );

            // Trả về qua layout (giữ sidebar/header), không trả thẳng fragment
            return "layouts/adminlte/layout";
        }

        try {
            product.setId(idGenerator.generateProductId());
            product.setCreatedBy(auth.getName());
            product.setUpdatedBy(auth.getName());
            product.setCreateDate(LocalDateTime.now());
            product.setUpdateDate(LocalDateTime.now());
            product.setDeleteFlag(false);
            product.setStatus(1);

            // Generate slug if not provided
            if (product.getSlug() == null || product.getSlug().isBlank()) {
                product.setSlug(SlugUtil.generateSlug(product.getName(), product.getId()));
            }

            // Check duplicate slug
            String originalSlug = product.getSlug();
            String finalSlug = originalSlug;
            int suffix = 1;

            // Kiểm tra và tạo slug unique
            while (true) {
                try {
                    Product existing = productService.findBySlug(finalSlug);
                    if (existing == null) break;
                    finalSlug = SlugUtil.generateUniqueSlug(originalSlug, suffix);
                    suffix++;
                    if (suffix > 100) {
                        throw new RuntimeException("Không thể tạo slug duy nhất sau 100 lần thử.");
                    }
                } catch (ResourceNotFoundException e) {
                    // Slug không tồn tại → dùng được
                    break;
                }
            }

            product.setSlug(finalSlug);
            productService.save(product);
            log.info("Sản phẩm '{}' (ID: {}) đã được thêm thành công", product.getName(), product.getId());
            ra.addFlashAttribute("success", "Thêm sản phẩm thành công!");
            return "redirect:/admin/products/" + product.getId() + "/details";

        } catch (Exception e) {
            log.error("Lỗi khi thêm sản phẩm: {}", e.getMessage(), e);
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            // Load lại dữ liệu cho form
            model.addAttribute("product", product);
            model.addAttribute("brands", brandService.findAll());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("materials", materialService.findAll());
            model.addAttribute("pageContent", "admin/product/create");
            model.addAttribute("title", "Thêm sản phẩm");
            return "layouts/adminlte/layout";
        }
    }

    // ============================================================
    // EDIT PRODUCT
    // ============================================================

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        model.addAttribute("product",    productService.findById(id));
        model.addAttribute("brands",     brandService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("materials",  materialService.findAll());
        model.addAttribute("pageContent", "admin/product/edit");
        model.addAttribute("title", "Sửa sản phẩm");
        return "layouts/adminlte/layout";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable String id,
                         @Valid @ModelAttribute Product product,
                         BindingResult bindingResult,
                         Authentication auth,
                         Model model,  // ✅ Thêm Model
                         RedirectAttributes ra) {

        // ✅ Kiểm tra validation trước
        if (bindingResult.hasErrors()) {
            // Load lại dữ liệu
            model.addAttribute("product", product);
            model.addAttribute("brands", brandService.findAll());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("materials", materialService.findAll());
            model.addAttribute("pageContent", "admin/product/edit");
            model.addAttribute("title", "Sửa sản phẩm");

            bindingResult.getAllErrors().forEach(error ->
                    log.warn("Validation error on update: {}", error.getDefaultMessage())
            );

            return "layouts/adminlte/layout";  // Giữ layout, không trả thẳng fragment
        }

        try {
            Product existingProduct = productService.findById(id);

            // Update fields
            existingProduct.setName(product.getName());
            existingProduct.setDescription(product.getDescription());
            existingProduct.setGender(product.getGender());
            existingProduct.setStatus(product.getStatus() != null ? product.getStatus() : 1);
            existingProduct.setBrand(product.getBrand());
            existingProduct.setCategory(product.getCategory());
            existingProduct.setMaterial(product.getMaterial());
            existingProduct.setUpdatedBy(auth.getName());
            existingProduct.setUpdateDate(LocalDateTime.now());

            // Handle slug changes
            String newSlug = product.getSlug();
            if (newSlug == null || newSlug.isBlank()) {
                newSlug = SlugUtil.generateSlug(product.getName(), id);
            }

            if (!SlugUtil.slugEquals(existingProduct.getSlug(), newSlug)) {
                String originalSlug = newSlug;
                String finalSlug = newSlug;
                int suffix = 1;

                while (true) {
                    try {
                        Product conflictProduct = productService.findBySlug(finalSlug);
                        if (conflictProduct == null || conflictProduct.getId().equals(id)) {
                            break;
                        }
                        finalSlug = SlugUtil.generateUniqueSlug(originalSlug, suffix);
                        suffix++;
                        if (suffix > 100) {
                            throw new RuntimeException("Không thể tạo slug duy nhất sau 100 lần thử.");
                        }
                    } catch (ResourceNotFoundException e) {
                        break;
                    }
                }
                existingProduct.setSlug(finalSlug);
            }

            productService.update(existingProduct);
            log.info("Sản phẩm '{}' (ID: {}) đã được cập nhật thành công", existingProduct.getName(), id);
            ra.addFlashAttribute("success", "Cập nhật sản phẩm thành công!");

        } catch (Exception e) {
            log.error("Lỗi khi cập nhật sản phẩm ID {}: {}", id, e.getMessage(), e);
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            // Load lại dữ liệu
            product.setId(id);
            model.addAttribute("product", product);
            model.addAttribute("brands", brandService.findAll());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("materials", materialService.findAll());
            model.addAttribute("pageContent", "admin/product/edit");
            model.addAttribute("title", "Sửa sản phẩm");
            return "layouts/adminlte/layout";
        }
        return "redirect:/admin/products/" + id + "/details";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes ra) {
        try {
            Product product = productService.findById(id);
            productService.delete(id);
            log.info("Sản phẩm '{}' (ID: {}) đã được xóa thành công", product.getName(), id);
            ra.addFlashAttribute("success", "Xóa sản phẩm thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi xóa sản phẩm ID {}: {}", id, e.getMessage(), e);
            ra.addFlashAttribute("error", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/products/list";
    }

    // ============================================================
    // PRODUCT DETAIL (Biến thể size + màu)
    // ============================================================

    @GetMapping("/{id}/details")
    public String detailList(@PathVariable String id, Model model) {
        Product product = productService.findById(id);
        List<ProductDetail> details = productDetailService.findByProductId(id);

        // Mục 17: cố định nhóm bảng biến thể theo size — mỗi size 1 bảng con,
        // biến thể không có size xếp vào nhóm "Chưa gán size" ở cuối.
        java.util.LinkedHashMap<String, List<ProductDetail>> groupedBySize = new java.util.LinkedHashMap<>();
        for (ProductDetail d : details) {
            String key = d.getSize() != null ? d.getSize().getName() : "__NO_SIZE__";
            groupedBySize.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(d);
        }
        if (groupedBySize.containsKey("__NO_SIZE__")) {
            List<ProductDetail> noSize = groupedBySize.remove("__NO_SIZE__");
            groupedBySize.put("Chưa gán size", noSize);
        }

        model.addAttribute("product",    product);
        model.addAttribute("details",    details);
        model.addAttribute("groupedBySize", groupedBySize);
        model.addAttribute("sizes",      sizeService.findAll());
        model.addAttribute("colors",     colorService.findAll());
        model.addAttribute("newDetail",  new ProductDetail());
        model.addAttribute("images",     imageService.findByProductId(id));
        model.addAttribute("pageContent", "admin/product/detail");
        model.addAttribute("title", "Biến thể sản phẩm");
        return "layouts/adminlte/layout";
    }

    // ============================================================
    // ẢNH SẢN PHẨM
    // ============================================================

    @PostMapping("/{id}/images/upload")
    public String uploadImages(@PathVariable String id,
                               @RequestParam(value = "images", required = false) List<MultipartFile> images,
                               RedirectAttributes ra) {
        try {
            if (images == null || images.isEmpty() || images.stream().allMatch(MultipartFile::isEmpty)) {
                ra.addFlashAttribute("error", "Vui lòng chọn ít nhất 1 ảnh để upload");
            } else {
                imageService.uploadImages(id, images);
                ra.addFlashAttribute("success", "Đã upload ảnh thành công");
            }
        } catch (Exception e) {
            log.error("Lỗi upload ảnh sản phẩm {}: {}", id, e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi khi upload ảnh: " + e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/details";
    }

    @PostMapping("/{id}/images/{imageId}/delete")
    public String deleteImage(@PathVariable String id, @PathVariable Integer imageId, RedirectAttributes ra) {
        try {
            imageService.deleteImage(imageId);
            ra.addFlashAttribute("success", "Đã xoá ảnh");
        } catch (Exception e) {
            log.error("Lỗi xoá ảnh {}: {}", imageId, e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi khi xoá ảnh: " + e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/details";
    }

    @PostMapping("/{id}/images/{imageId}/set-thumbnail")
    public String setThumbnailImage(@PathVariable String id, @PathVariable Integer imageId, RedirectAttributes ra) {
        try {
            imageService.setThumbnail(imageId);
            ra.addFlashAttribute("success", "Đã đặt ảnh đại diện");
        } catch (Exception e) {
            log.error("Lỗi đặt ảnh đại diện {}: {}", imageId, e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/details";
    }

    @PostMapping("/{productId}/details/save")
    public String detailSave(@PathVariable String productId,
                             @ModelAttribute ProductDetail detail,
                             Authentication auth,
                             RedirectAttributes ra) {
        try {
            Product product = productService.findById(productId);
            detail.setProduct(product);
            detail.setCreatedBy(auth.getName());
            detail.setUpdatedBy(auth.getName());
            detail.setCreateDate(LocalDateTime.now());
            detail.setUpdateDate(LocalDateTime.now());
            detail.setDeleteFlag(false);
            detail.setStatus(1);
            if (detail.getQuantity() == null) detail.setQuantity(0);
            productDetailService.save(detail);
            ra.addFlashAttribute("success", "Thêm biến thể thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/details";
    }

    @PostMapping("/{productId}/details/{detailId}/update")
    public String detailUpdate(@PathVariable String productId,
                               @PathVariable Integer detailId,
                               @ModelAttribute ProductDetail detail,
                               Authentication auth,
                               RedirectAttributes ra) {
        try {
            detail.setId(detailId);
            detail.setUpdatedBy(auth.getName());
            productDetailService.update(detail);
            ra.addFlashAttribute("success", "Cập nhật biến thể thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/details";
    }

    @PostMapping("/{productId}/details/{detailId}/delete")
    public String detailDelete(@PathVariable String productId,
                               @PathVariable Integer detailId,
                               RedirectAttributes ra) {
        try {
            productDetailService.delete(detailId);
            ra.addFlashAttribute("success", "Xóa biến thể thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/details";
    }
}