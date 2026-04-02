package com.ducthin.LetCookGear.controller;

import com.ducthin.LetCookGear.dto.AuthRequest;
import com.ducthin.LetCookGear.dto.AuthMeResponse;
import com.ducthin.LetCookGear.dto.AuthResponse;
import com.ducthin.LetCookGear.dto.ApiResponse;
import com.ducthin.LetCookGear.dto.RegisterRequest;
import com.ducthin.LetCookGear.service.AuthService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", authService.login(request)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký thành công", authService.register(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthMeResponse>> me(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Thông tin người dùng hiện tại", authService.me(principal.getName())));
    }
}
