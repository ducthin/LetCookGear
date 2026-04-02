package com.ducthin.LetCookGear.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthMeResponse {
    private String email;
    private String fullName;
    private String phone;
    private List<String> roles;
}
