package com.smartinvoice.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class LoginResponse {

    private UUID businessId;
    private String businessName;
}