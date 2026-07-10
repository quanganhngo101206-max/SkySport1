package com.example.skysport1.repository;

import com.example.skysport1.entity.DiscountCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Integer> {
    Optional<DiscountCode> findByCode(String code);
    boolean existsByCode(String code);

    // Khoá dòng (SELECT ... FOR UPDATE) khi đọc, giữ khoá xuyên suốt transaction
    // tạo đơn hàng, tránh 2 đơn cùng dùng 1 voucher sắp hết lượt tại cùng thời
    // điểm đều đọc được usedCount < quantity rồi cùng tăng lên -> vượt quá số
    // lượt cho phép. Đối xứng với ProductDetailRepository.findByIdForUpdate().
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT dc FROM DiscountCode dc WHERE dc.code = :code")
    Optional<DiscountCode> findByCodeForUpdate(@Param("code") String code);
}