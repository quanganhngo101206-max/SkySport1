package com.example.skysport1.service.impl;

import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.BillDetail;
import com.example.skysport1.entity.Customer;
import com.example.skysport1.entity.Review;
import com.example.skysport1.enums.OrderStatus;
import com.example.skysport1.exception.AppException;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.repository.BillDetailRepository;
import com.example.skysport1.repository.CustomerRepository;
import com.example.skysport1.repository.ReviewRepository;
import com.example.skysport1.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BillDetailRepository billDetailRepository;
    private final CustomerRepository customerRepository;

    @Override
    public List<Review> findByCustomerId(String customerId) {
        return reviewRepository.findByCustomerId(customerId);
    }

    @Override
    public Review findById(Integer id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("đánh giá", String.valueOf(id)));
    }

    @Override
    @Transactional
    public Review create(String customerId, Integer billDetailId, Integer rating, String comment) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("khách hàng", customerId));

        BillDetail billDetail = billDetailRepository.findById(billDetailId)
                .orElseThrow(() -> new ResourceNotFoundException("chi tiết đơn hàng", String.valueOf(billDetailId)));

        // Chỉ cho đánh giá sản phẩm thuộc đơn hàng của CHÍNH khách hàng này —
        // tránh việc customer A đoán/gõ billDetailId của đơn customer B rồi
        // review hộ. Đơn guest (bill.customer == null) sẽ luôn fail check này,
        // đúng ý vì guest không có tài khoản để "đánh giá" được.
        Bill bill = billDetail.getBill();
        if (bill == null || bill.getCustomer() == null
                || !customerId.equals(bill.getCustomer().getId())) {
            throw new AppException("Bạn không có quyền đánh giá sản phẩm này");
        }

        // Chỉ cho đánh giá khi đơn đã ở trạng thái Hoàn thành (COMPLETED) —
        // trước đây thiếu điều kiện này nên có thể review ngay cả khi đơn
        // đang PENDING/SHIPPING, chưa chắc khách đã thực sự nhận & dùng sản phẩm.
        if (!OrderStatus.COMPLETED.matches(bill.getStatus())) {
            throw new AppException("Chỉ có thể đánh giá sản phẩm khi đơn hàng đã hoàn thành");
        }

        if (reviewRepository.existsByBillDetailIdAndCustomerId(billDetailId, customerId)) {
            throw new AppException("Bạn đã đánh giá sản phẩm này rồi");
        }

        Review review = Review.builder()
                .billDetail(billDetail)
                .customer(customer)
                .rating(rating)
                .comment(comment)
                .status(1)
                .build();

        log.info("Created review for billDetail {} by customer {}", billDetailId, customerId);
        return reviewRepository.save(review);
    }

    @Override
    @Transactional
    public Review update(Integer reviewId, Integer rating, String comment) {
        Review review = findById(reviewId);
        review.setRating(rating);
        review.setComment(comment);
        log.info("Updated review {}", reviewId);
        return reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void delete(Integer reviewId) {
        Review review = findById(reviewId);
        review.setStatus(0);
        reviewRepository.save(review);
        log.info("Soft deleted review {}", reviewId);
    }
}