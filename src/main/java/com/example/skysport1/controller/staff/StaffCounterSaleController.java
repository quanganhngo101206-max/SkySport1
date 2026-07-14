package com.example.skysport1.controller.staff;

import com.example.skysport1.dto.request.CounterSaleCheckoutRequest;
import com.example.skysport1.dto.response.CounterSaleCustomerResponse;
import com.example.skysport1.dto.response.CounterSaleProductResponse;
import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.BillDetail;
import com.example.skysport1.entity.Customer;
import com.example.skysport1.entity.Payment;
import com.example.skysport1.entity.ProductDetail;
import com.example.skysport1.exception.AppException;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.CustomerRepository;
import com.example.skysport1.repository.PaymentRepository;
import com.example.skysport1.repository.ProductDetailRepository;
import com.example.skysport1.service.BillService;
import com.example.skysport1.service.BrandService;
import com.example.skysport1.service.CategoryService;
import com.example.skysport1.service.ColorService;
import com.example.skysport1.service.MaterialService;
import com.example.skysport1.service.SizeService;
import com.example.skysport1.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bán hàng tại quầy. Dùng chung cho cả Staff và Admin vì /staff/** cho phép
 * cả 2 role (xem SecurityConfig). Tạo đơn hoàn thành ngay, thanh toán ngay
 * (invoiceType = 2), tái sử dụng BillService.createCounterBill() đã có sẵn.
 */
@Controller
@RequestMapping("/staff/counter-sale")
@RequiredArgsConstructor
@Slf4j
public class StaffCounterSaleController {

    private final BillService billService;
    private final StaffService staffService;
    private final ProductDetailRepository productDetailRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final MaterialService materialService;
    private final SizeService sizeService;
    private final ColorService colorService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("payments", paymentRepository.findAll());
        model.addAttribute("brands", brandService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("materials", materialService.findAll());
        model.addAttribute("sizes", sizeService.findAll());
        model.addAttribute("colors", colorService.findAll());
        model.addAttribute("title", "Bán hàng tại quầy");
        model.addAttribute("pageContent", "staff/counter-sale/index");
        return "layouts/staff/layout";
    }

    /**
     * Lọc sản phẩm kết hợp theo hãng/danh mục/chất liệu/size/màu + từ khoá.
     * Mọi tham số optional, không truyền hoặc rỗng nghĩa là bỏ qua điều kiện đó.
     */
    @GetMapping("/filter-product")
    @ResponseBody
    public List<CounterSaleProductResponse> filterProduct(
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String materialId,
            @RequestParam(required = false) String sizeId,
            @RequestParam(required = false) String colorId,
            @RequestParam(required = false) String keyword) {
        List<ProductDetail> results = productDetailRepository.filterForCounterSale(
                blankToNull(brandId), blankToNull(categoryId), blankToNull(materialId),
                blankToNull(sizeId), blankToNull(colorId), keyword);
        return results.stream()
                .map(pd -> new CounterSaleProductResponse(
                        pd.getId(),
                        pd.getSku(),
                        pd.getProduct() != null ? pd.getProduct().getName() : "",
                        pd.getSize() != null ? pd.getSize().getName() : null,
                        pd.getColor() != null ? pd.getColor().getName() : null,
                        pd.getPrice(),
                        pd.getQuantity()
                ))
                .collect(Collectors.toList());
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /**
     * Tìm sản phẩm theo SKU hoặc tên, dùng cho ô tìm kiếm trên trang bán hàng.
     */
    @GetMapping("/search-product")
    @ResponseBody
    public List<CounterSaleProductResponse> searchProduct(@RequestParam String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        List<ProductDetail> results = productDetailRepository.searchForCounterSale(keyword.trim());
        return results.stream()
                .map(pd -> new CounterSaleProductResponse(
                        pd.getId(),
                        pd.getSku(),
                        pd.getProduct() != null ? pd.getProduct().getName() : "",
                        pd.getSize() != null ? pd.getSize().getName() : null,
                        pd.getColor() != null ? pd.getColor().getName() : null,
                        pd.getPrice(),
                        pd.getQuantity()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Tra cứu khách hàng theo số điện thoại. Không tìm thấy -> trả 404 rỗng,
     * frontend hiểu là "khách vãng lai".
     */
    @GetMapping("/search-customer")
    @ResponseBody
    public ResponseEntity<CounterSaleCustomerResponse> searchCustomer(@RequestParam String phone) {
        return customerRepository.findByPhone(phone)
                .map(c -> ResponseEntity.ok(new CounterSaleCustomerResponse(c.getId(), c.getFullName(), c.getPhone())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/checkout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkout(@Valid @RequestBody CounterSaleCheckoutRequest request,
                                                        BindingResult bindingResult,
                                                        Authentication auth) {
        Map<String, Object> body = new HashMap<>();

        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            body.put("success", false);
            body.put("message", msg);
            return ResponseEntity.badRequest().body(body);
        }

        String staffId = null;
        try {
            staffId = staffService.findByAccountUsername(auth.getName()).getId();
        } catch (Exception e) {
            log.warn("Tài khoản {} không có Staff record, ghi log bằng username khi bán tại quầy", auth.getName());
        }

        // Khách vãng lai nếu không chọn customerId
        String customerId = null;
        if (request.getCustomerId() != null && !request.getCustomerId().isBlank()) {
            customerId = customerRepository.findById(request.getCustomerId())
                    .map(Customer::getId)
                    .orElse(null);
        }

        List<BillDetail> items = request.getItems().stream()
                .map(i -> BillDetail.builder()
                        .productDetail(ProductDetail.builder().id(i.getProductDetailId()).build())
                        .quantity(i.getQuantity())
                        .build())
                .collect(Collectors.toList());

        try {
            Bill bill = billService.createCounterBill(customerId, request.getPaymentId(), staffId, items);
            body.put("success", true);
            body.put("billId", bill.getId());
            body.put("totalAmount", bill.getTotalAmount());
            log.info("Bán hàng tại quầy thành công: {} (staff={}, customer={})", bill.getId(), staffId, customerId);
            return ResponseEntity.ok(body);
        } catch (ResourceNotFoundException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        } catch (AppException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.status(e.getStatus()).body(body);
        } catch (Exception e) {
            log.error("Lỗi khi bán hàng tại quầy: {}", e.getMessage(), e);
            body.put("success", false);
            body.put("message", "Có lỗi xảy ra, vui lòng thử lại: " + e.getMessage());
            return ResponseEntity.internalServerError().body(body);
        }
    }
}