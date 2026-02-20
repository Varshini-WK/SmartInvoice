package com.smartinvoice.backend.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank
    private String newname;

    @NotBlank
    private String password;

    public String getName() {
        return newname;
    }
}