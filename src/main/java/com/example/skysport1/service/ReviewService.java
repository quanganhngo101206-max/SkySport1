package com.example.skysport1.service;

import com.example.skysport1.entity.Review;
import java.util.List;

public interface ReviewService {

    List<Review> findByCustomerId(String customerId);

    Review findById(Integer id);

    Review create(String customerId, Integer billDetailId, Integer rating, String comment);

    Review update(Integer reviewId, Integer rating, String comment);

    void delete(Integer reviewId);
}