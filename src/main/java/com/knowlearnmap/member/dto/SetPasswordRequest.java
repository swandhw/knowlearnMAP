package com.knowlearnmap.member.dto;

import lombok.Data;

@Data
public class SetPasswordRequest {
    private String token;
    private String password;
}
