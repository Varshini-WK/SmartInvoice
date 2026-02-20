package com.smartinvoice.backend.service;

import com.smartinvoice.backend.domain.Business;
import com.smartinvoice.backend.dto.LoginRequest;
import com.smartinvoice.backend.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.smartinvoice.backend.repository.BusinessRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final BusinessRepository businessRepository;

    public LoginResponse login(LoginRequest request) {

        Business business = businessRepository
                .findByName(request.getName())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!business.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return LoginResponse.builder()
                .businessId(business.getId())
                .businessName(business.getName())
                .build();
    }
}
