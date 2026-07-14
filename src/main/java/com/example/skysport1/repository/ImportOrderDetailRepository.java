package com.example.skysport1.repository;

import com.example.skysport1.entity.ImportOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportOrderDetailRepository extends JpaRepository<ImportOrderDetail, Integer> {

    @Query("""
           select d
           from ImportOrderDetail d
           left join fetch d.productDetail pd
           left join fetch pd.product p
           left join fetch pd.size s
           left join fetch pd.color c
           where d.importOrder.id = :importOrderId
           """)
    List<ImportOrderDetail> findByImportOrderId(@Param("importOrderId") String importOrderId);

    @Query("""
           select d
           from ImportOrderDetail d
           left join fetch d.productDetail pd
           left join fetch pd.product p
           left join fetch pd.size s
           left join fetch pd.color c
           where d.productDetail.id = :productDetailId
           """)
    List<ImportOrderDetail> findByProductDetailId(@Param("productDetailId") Integer productDetailId);

    // Dùng khi sửa phiếu nhập đang Chờ duyệt: xoá hết chi tiết cũ trước khi
    // ghi lại chi tiết mới. An toàn vì phiếu Chờ duyệt chưa cộng tồn kho.
    void deleteByImportOrderId(String importOrderId);
}