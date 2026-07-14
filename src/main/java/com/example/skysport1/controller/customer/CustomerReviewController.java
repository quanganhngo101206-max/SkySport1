package com.example.skysport1.controller.customer;

import com.example.skysport1.dto.request.ReviewRequest;
import com.example.skysport1.entity.Account;
import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.BillDetail;
import com.example.skysport1.entity.Customer;
import com.example.skysport1.entity.Review;
import com.example.skysport1.enums.OrderStatus;
import com.example.skysport1.repository.BillDetailRepository;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.service.CustomerService;
import com.example.skysport1.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customer/reviews")
@RequiredArgsConstructor
@Slf4j
public class CustomerReviewController {

    private final ReviewService reviewService;
    private final BillDetailRepository billDetailRepository;
    private final CustomerService customerService;
    private final AccountService accountService;

    /**
     * Danh sách đánh giá của tôi
     */
    @GetMapping
    public String list(Authentication auth, Model model) {
        String customerId = getCurrentCustomerId(auth);
        List<Review> reviews = reviewService.findByCustomerId(customerId);

        model.addAttribute("reviews", reviews);
        model.addAttribute("title", "Đánh giá của tôi");
        model.addAttribute("pageContent", "customer/review/list");
        return "layouts/customer/layout";
    }

    /**
     * Form viết đánh giá
     */
    @GetMapping("/create/{billDetailId}")
    public String createForm(@PathVariable Integer billDetailId, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            String customerId = getCurrentCustomerId(auth);
            BillDetail billDetail = billDetailRepository.findById(billDetailId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            // Chặn sớm ở đây (thay vì để khách điền form xong mới báo lỗi ở
            // bước submit): phải đúng chủ đơn + đơn đã Hoàn thành mới cho
            // vào form đánh giá. Logic thật sự (không thể bỏ qua) vẫn nằm ở
            // ReviewServiceImpl.create().
            Bill bill = billDetail.getBill();
            if (bill == null || bill.getCustomer() == null
                    || !customerId.equals(bill.getCustomer().getId())) {
                throw new RuntimeException("Bạn không có quyền đánh giá sản phẩm này");
            }
            if (!OrderStatus.COMPLETED.matches(bill.getStatus())) {
                throw new RuntimeException("Chỉ có thể đánh giá sản phẩm khi đơn hàng đã hoàn thành");
            }

            model.addAttribute("billDetail", billDetail);
            model.addAttribute("reviewRequest", new ReviewRequest());
            model.addAttribute("title", "Viết đánh giá");
            model.addAttribute("pageContent", "customer/review/form");
            return "layouts/customer/layout";

        } catch (Exception e) {
            log.error("Error loading review form: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/orders";
        }
    }

    /**
     * Xử lý viết đánh giá
     */
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute ReviewRequest request,
                         @RequestParam Integer billDetailId,
                         BindingResult bindingResult,
                         Authentication auth,
                         Model model,
                         RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Viết đánh giá");
            model.addAttribute("pageContent", "customer/review/form");
            return "layouts/customer/layout";
        }

        try {
            String customerId = getCurrentCustomerId(auth);
            reviewService.create(customerId, billDetailId, request.getRating(), request.getComment());
            ra.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá sản phẩm!");
        } catch (Exception e) {
            log.error("Error creating review: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/reviews";
    }

    /**
     * Sửa đánh giá
     */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Review review = reviewService.findById(id);
            String customerId = getCurrentCustomerId(auth);

            if (!review.getCustomer().getId().equals(customerId)) {
                ra.addFlashAttribute("error", "Bạn không có quyền sửa đánh giá này");
                return "redirect:/customer/reviews";
            }

            ReviewRequest request = new ReviewRequest();
            request.setRating(review.getRating());
            request.setComment(review.getComment());

            model.addAttribute("review", review);
            model.addAttribute("reviewRequest", request);
            model.addAttribute("title", "Sửa đánh giá");
            model.addAttribute("pageContent", "customer/review/form");
            return "layouts/customer/layout";

        } catch (Exception e) {
            log.error("Error loading review edit: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/reviews";
        }
    }

    /**
     * Xử lý cập nhật đánh giá
     */
    @PostMapping("/update/{id}")
    public String update(@PathVariable Integer id,
                         @Valid @ModelAttribute ReviewRequest request,
                         BindingResult bindingResult,
                         Authentication auth,
                         Model model,
                         RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Sửa đánh giá");
            model.addAttribute("pageContent", "customer/review/form");
            return "layouts/customer/layout";
        }

        try {
            reviewService.update(id, request.getRating(), request.getComment());
            ra.addFlashAttribute("success", "Cập nhật đánh giá thành công!");
        } catch (Exception e) {
            log.error("Error updating review: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/reviews";
    }

    /**
     * Xóa đánh giá
     */
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, Authentication auth, RedirectAttributes ra) {
        try {
            reviewService.delete(id);
            ra.addFlashAttribute("success", "Xóa đánh giá thành công!");
        } catch (Exception e) {
            log.error("Error deleting review: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/reviews";
    }

    private String getCurrentCustomerId(Authentication auth) {
        String username = auth.getName();
        Account account = accountService.findByUsername(username);
        return customerService.findByAccountId(account.getId()).getId();
    }
}