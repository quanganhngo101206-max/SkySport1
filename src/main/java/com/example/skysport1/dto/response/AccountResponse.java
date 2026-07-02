package com.example.skysport1.dto.response;

import lombok.Data;

@Data
public class AccountResponse {
    private String id;
    private String username;
    private String email;
    private String roleName;
    private Integer status;
}