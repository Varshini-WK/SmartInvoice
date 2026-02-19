package com.smartinvoice.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CustomerResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
}