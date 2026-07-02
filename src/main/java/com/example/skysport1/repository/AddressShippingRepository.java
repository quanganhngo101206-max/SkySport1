package com.example.skysport1.repository;

import com.example.skysport1.entity.AddressShipping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AddressShippingRepository extends JpaRepository<AddressShipping, Integer> {
    List<AddressShipping> findByCustomerId(String customerId);
    Optional<AddressShipping> findByCustomerIdAndIsDefault(String customerId, Boolean isDefault);
}
