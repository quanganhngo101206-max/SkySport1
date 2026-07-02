package com.example.skysport1.service;

import com.example.skysport1.dto.request.RegisterRequest;
import com.example.skysport1.entity.Account;

public interface AccountService {
    Account register(RegisterRequest request);

    Account findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void updatePassword(String username, String encodedPassword);
}