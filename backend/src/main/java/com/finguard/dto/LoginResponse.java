package com.finguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;
    private String userId;
    private String name;
    private String email;
    private String role;
    private long expiresIn;  // milliseconds

    public static LoginResponse of(String token, String userId, String name,
                                    String email, String role, long expiresIn) {
        return LoginResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .userId(userId)
            .name(name)
            .email(email)
            .role(role)
            .expiresIn(expiresIn)
            .build();
    }
}
