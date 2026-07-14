package com.example.skysport1.repository;

import com.example.skysport1.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {

    Optional<Staff> findByAccountId(String accountId);

    Optional<Staff> findByPhone(String phone);

    List<Staff> findByDeleteFlagFalseOrderByFullNameAsc();

    List<Staff> findByStatusAndDeleteFlagFalse(Integer status);

    boolean existsByPhone(String phone);

    @Query("SELECT s FROM Staff s WHERE s.deleteFlag = false AND " +
            "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Staff> searchByName(@Param("keyword") String keyword);

    @Query("SELECT s FROM Staff s WHERE s.account.username = :username")
    Optional<Staff> findByAccountUsername(@Param("username") String username);

    @Query("SELECT s FROM Staff s WHERE s.account.role.name = 'ADMIN' " +
            "AND s.deleteFlag = false AND s.status = 1")
    List<Staff> findAllActiveAdmins();
}