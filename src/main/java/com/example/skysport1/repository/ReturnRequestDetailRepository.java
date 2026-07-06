package com.example.skysport1.repository;

import com.example.skysport1.entity.ReturnRequestDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestDetailRepository extends JpaRepository<ReturnRequestDetail, Integer> {

    List<ReturnRequestDetail> findByReturnRequestId(String returnRequestId);

    // Dùng cho trang chi tiết yêu cầu hoàn trả (admin): nạp sẵn productDetail + product + size + color
    // để tránh LazyInitializationException khi Thymeleaf render ngoài transaction.
    @Query("SELECT d FROM ReturnRequestDetail d " +
            "LEFT JOIN FETCH d.productDetail pd " +
            "LEFT JOIN FETCH pd.product " +
            "LEFT JOIN FETCH pd.size " +
            "LEFT JOIN FETCH pd.color " +
            "WHERE d.returnRequest.id = :returnRequestId")
    List<ReturnRequestDetail> findByReturnRequestIdWithProductSizeColor(@Param("returnRequestId") String returnRequestId);
}