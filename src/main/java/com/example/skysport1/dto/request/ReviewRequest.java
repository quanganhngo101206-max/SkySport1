package com.example.skysport1.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull(message = "Vui lòng chọn đánh giá")
    @Min(value = 1, message = "Đánh giá từ 1-5 sao")
    @Max(value = 5, message = "Đánh giá từ 1-5 sao")
    private Integer rating;

    private String comment;
}