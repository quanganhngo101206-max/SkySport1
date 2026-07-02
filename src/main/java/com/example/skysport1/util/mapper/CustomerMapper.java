package com.example.skysport1.util.mapper;

import com.example.skysport1.dto.response.CustomerResponse;
import com.example.skysport1.entity.Customer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) return null;

        return CustomerResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .genderLabel(customer.getGender() != null
                        ? (customer.getGender() ? "Nam" : "Nữ")
                        : "Chưa xác định")
                .dob(customer.getDob())
                .status(customer.getStatus())
                .createDate(customer.getCreateDate())
                .build();
    }

    public List<CustomerResponse> toResponses(List<Customer> customers) {
        if (customers == null) return List.of();
        return customers.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
