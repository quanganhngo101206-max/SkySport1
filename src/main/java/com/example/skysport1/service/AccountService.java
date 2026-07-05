package com.example.skysport1.service;

import com.example.skysport1.dto.request.RegisterRequest;
import com.example.skysport1.entity.Account;
import org.springframework.data.repository.query.Param;

public interface AccountService {
    Account register(RegisterRequest request);

    Account findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void updatePassword(String username, String encodedPassword);

    Account findByUsernameWithRole(@Param("username") String username);
}