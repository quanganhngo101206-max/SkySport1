package com.example.skysport1.controller.admin;

import com.example.skysport1.entity.*;
import com.example.skysport1.service.*;
import com.example.skysport1.util.IdGenerator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Quản lý các thuộc tính sản phẩm:
 * Brand, Category, Material, Size, Color
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminAttributeController {

    private final BrandService    brandService;
    private final CategoryService categoryService;
    private final MaterialService materialService;
    private final SizeService     sizeService;
    private final ColorService    colorService;
    private final IdGenerator     idGenerator;

    // ============================================================
    // BRAND
    // ============================================================

    @GetMapping("/brands")
    public String brandList(Model model) {
        model.addAttribute("brands", brandService.findAll());
        model.addAttribute("brand", new Brand());
        model.addAttribute("pageContent", "admin/brand/list");
        model.addAttribute("title", "Thương hiệu");
        return "layouts/adminlte/layout";
    }

    @PostMapping("/brands/save")
    public String brandSave(@Valid @ModelAttribute Brand brand,
                            BindingResult bindingResult,
                            Authentication auth,
                            RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            log.warn("Validation failed when saving brand: {}", bindingResult.getAllErrors());
            ra.addFlashAttribute("error", "Lỗi validation: kiểm tra lại các trường.");
            return "redirect:/admin/brands";
        }
        try {
            brand.setId(idGenerator.generateBrandId());
            brand.setCreatedBy(auth.getName());
            brand.setUpdatedBy(auth.getName());
            brandService.save(brand);
            ra.addFlashAttribute("success", "Thêm thương hiệu thành công!");
            log.info("Brand '{}' created by {}", brand.getName(), auth.getName());
        } catch (Exception e) {
            log.error("Error saving brand: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/brands";
    }

    @PostMapping("/brands/update")
    public String brandUpdate(@Valid @ModelAttribute Brand brand,
                              BindingResult bindingResult,
                              Authentication auth,
                              RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            log.warn("Validation failed when updating brand: {}", bindingResult.getAllErrors());
            ra.addFlashAttribute("error", "Lỗi validation: kiểm tra lại các trường.");
            return "redirect:/admin/brands";
        }
        try {
            brand.setUpdatedBy(auth.getName());
            brandService.update(brand);
            ra.addFlashAttribute("success", "Cập nhật thương hiệu thành công!");
            log.info("Brand '{}' updated by {}", brand.getName(), auth.getName());
        } catch (Exception e) {
            log.error("Error updating brand: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/brands";
    }

    @PostMapping("/brands/delete/{id}")
    public String brandDelete(@PathVariable String id, RedirectAttributes ra) {
        try {
            brandService.delete(id);
            ra.addFlashAttribute("success", "Xóa thương hiệu thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/brands";
    }

    // ============================================================
    // CATEGORY
    // ============================================================

    @GetMapping("/categories")
    public String categoryList(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("category", new Category());
        model.addAttribute("pageContent", "admin/category/list");
        model.addAttribute("title", "Danh mục");
        return "layouts/adminlte/layout";
    }

    @PostMapping("/categories/save")
    public String categorySave(@Valid @ModelAttribute Category category,
                               BindingResult bindingResult,
                               Authentication auth,
                               RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            log.warn("Validation failed when saving category: {}", bindingResult.getAllErrors());
            ra.addFlashAttribute("error", "Lỗi validation: kiểm tra lại các trường.");
            return "redirect:/admin/categories";
        }
        try {
            category.setId(idGenerator.generateCategoryId());
            category.setCreatedBy(auth.getName());
            category.setUpdatedBy(auth.getName());
            categoryService.save(category);
            ra.addFlashAttribute("success", "Thêm danh mục thành công!");
            log.info("Category '{}' created by {}", category.getName(), auth.getName());
        } catch (Exception e) {
            log.error("Error saving category: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/update")
    public String categoryUpdate(@Valid @ModelAttribute Category category,
                                 BindingResult bindingResult,
                                 Authentication auth,
                                 RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            log.warn("Validation failed when updating category: {}", bindingResult.getAllErrors());
            ra.addFlashAttribute("error", "Lỗi validation: kiểm tra lại các trường.");
            return "redirect:/admin/categories";
        }
        try {
            category.setUpdatedBy(auth.getName());
            categoryService.update(category);
            ra.addFlashAttribute("success", "Cập nhật danh mục thành công!");
            log.info("Category '{}' updated by {}", category.getName(), auth.getName());
        } catch (Exception e) {
            log.error("Error updating category: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/delete/{id}")
    public String categoryDelete(@PathVariable String id, RedirectAttributes ra) {
        try {
            categoryService.delete(id);
            ra.addFlashAttribute("success", "Xóa danh mục thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ============================================================
    // MATERIAL
    // ============================================================

    @GetMapping("/materials")
    public String materialList(Model model) {
        model.addAttribute("materials", materialService.findAll());
        model.addAttribute("material", new Material());
        model.addAttribute("pageContent", "admin/material/list");
        model.addAttribute("title", "Chất liệu");
        return "layouts/adminlte/layout";
    }

    @PostMapping("/materials/save")
    public String materialSave(@Valid @ModelAttribute Material material,
                               BindingResult bindingResult,
                               Authentication auth,
                               RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            log.warn("Validation failed when saving material: {}", bindingResult.getAllErrors());
            ra.addFlashAttribute("error", "Lỗi validation: kiểm tra lại các trường.");
            return "redirect:/admin/materials";
        }
        try {
            material.setId(idGenerator.generateMaterialId());
            material.setCreatedBy(auth.getName());
            material.setUpdatedBy(auth.getName());
            materialService.save(material);
            ra.addFlashAttribute("success", "Thêm chất liệu thành công!");
            log.info("Material '{}' created by {}", material.getName(), auth.getName());
        } catch (Exception e) {
            log.error("Error saving material: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/materials";
    }

    @PostMapping("/materials/update")
    public String materialUpdate(@Valid @ModelAttribute Material material,
                                 BindingResult bindingResult,
                                 Authentication auth,
                                 RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            log.warn("Validation failed when updating material: {}", bindingResult.getAllErrors());
            ra.addFlashAttribute("error", "Lỗi validation: kiểm tra lại các trường.");
            return "redirect:/admin/materials";
        }
        try {
            material.setUpdatedBy(auth.getName());
            materialService.update(material);
            ra.addFlashAttribute("success", "Cập nhật chất liệu thành công!");
            log.info("Material '{}' updated by {}", material.getName(), auth.getName());
        } catch (Exception e) {
            log.error("Error updating material: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/materials";
    }

    @PostMapping("/materials/delete/{id}")
    public String materialDelete(@PathVariable String id, RedirectAttributes ra) {
        try {
            materialService.delete(id);
            ra.addFlashAttribute("success", "Xóa chất liệu thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/materials";
    }

    // ============================================================
    // SIZE
    // ============================================================

    @GetMapping("/sizes")
    public String sizeList(Model model) {
        model.addAttribute("sizes", sizeService.findAll());
        model.addAttribute("size", new Size());
        model.addAttribute("pageContent", "admin/size/list");
        model.addAttribute("title", "Kích cỡ");
        return "layouts/adminlte/layout";
    }

    @PostMapping("/sizes/save")
    public String sizeSave(@Valid @ModelAttribute Size size,
                           BindingResult bindingResult,
                           Authentication auth,
                           RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            log.warn("Validation failed when saving size: {}", bindingResult.getAllErrors());
            ra.addFlashAttribute("error", "Lỗi validation: kiểm tra lại các trường.");
            return "redirect:/admin/sizes";
        }
        try {
            size.setId(idGenerator.generateSizeId());
            sizeService.save(size);
            ra.addFlashAttribute("success", "Thêm size thành công!");
            log.info("Size '{}' created", size.getName());
        } catch (Exception e) {
            log.error("Error saving size: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/sizes";
    }

    @PostMapping("/sizes/update")
    public String sizeUpdate(@Valid @ModelAttribute Size size,
                             BindingResult bindingResult,
                             RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            log.warn("Validation failed when updating size: {}", bindingResult.getAllErrors());
            ra.addFlashAttribute("error", "Lỗi validation: kiểm tra lại các trường.");
            return "redirect:/admin/sizes";
        }
        try {
            sizeService.update(size);
            ra.addFlashAttribute("success", "Cập nhật size thành công!");
            log.info("Size '{}' updated", size.getName());
        } catch (Exception e) {
            log.error("Error updating size: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/sizes";
    }

    @PostMapping("/sizes/delete/{id}")
    public String sizeDelete(@PathVariable String id, RedirectAttributes ra) {
        try {
            sizeService.delete(id);
            ra.addFlashAttribute("success", "Xóa size thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/sizes";
    }

    // ============================================================
    // COLOR
    // ============================================================

    @GetMapping("/colors")
    public String colorList(Model model) {
        model.addAttribute("colors", colorService.findAll());
        model.addAttribute("color", new Color());
        model.addAttribute("pageContent", "admin/color/list");
        model.addAttribute("title", "Màu sắc");
        return "layouts/adminlte/layout";
    }

    @PostMapping("/colors/save")
    public String colorSave(@Valid @ModelAttribute Color color,
                            BindingResult bindingResult,
                            Authentication auth,
                            RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            log.warn("Validation failed when saving color: {}", bindingResult.getAllErrors());
            ra.addFlashAttribute("error", "Lỗi validation: kiểm tra lại các trường.");
            return "redirect:/admin/colors";
        }
        try {
            color.setId(idGenerator.generateColorId());
            colorService.save(color);
            ra.addFlashAttribute("success", "Thêm màu thành công!");
            log.info("Color '{}' created", color.getName());
        } catch (Exception e) {
            log.error("Error saving color: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/colors";
    }

    @PostMapping("/colors/update")
    public String colorUpdate(@Valid @ModelAttribute Color color,
                              BindingResult bindingResult,
                              RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            log.warn("Validation failed when updating color: {}", bindingResult.getAllErrors());
            ra.addFlashAttribute("error", "Lỗi validation: kiểm tra lại các trường.");
            return "redirect:/admin/colors";
        }
        try {
            colorService.update(color);
            ra.addFlashAttribute("success", "Cập nhật màu thành công!");
            log.info("Color '{}' updated", color.getName());
        } catch (Exception e) {
            log.error("Error updating color: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/colors";
    }

    @PostMapping("/colors/delete/{id}")
    public String colorDelete(@PathVariable String id, RedirectAttributes ra) {
        try {
            colorService.delete(id);
            ra.addFlashAttribute("success", "Xóa màu thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/colors";
    }
}