package com.ducthin.LetCookGear.service;

import com.ducthin.LetCookGear.dto.AuthRequest;
import com.ducthin.LetCookGear.dto.AuthMeResponse;
import com.ducthin.LetCookGear.dto.AuthResponse;
import com.ducthin.LetCookGear.dto.RegisterRequest;
import com.ducthin.LetCookGear.entity.Role;
import com.ducthin.LetCookGear.entity.User;
import com.ducthin.LetCookGear.entity.enums.UserStatus;
import com.ducthin.LetCookGear.repository.RoleRepository;
import com.ducthin.LetCookGear.repository.UserRepository;
import com.ducthin.LetCookGear.security.JwtService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository
                .findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Thông tin đăng nhập không hợp lệ"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String token = jwtService.generateToken(userDetails, Map.of("roles", roles, "name", user.getFullName()));

        return new AuthResponse(
                token, "Bearer", jwtService.getExpirationMs(), user.getEmail(), user.getFullName(), roles);
    }

    public AuthMeResponse me(String email) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        return new AuthMeResponse(user.getEmail(), user.getFullName(), user.getPhone(), roles);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        Role customerRole = roleRepository.findByName("CUSTOMER").orElseGet(() -> {
            Role role = new Role();
            role.setName("CUSTOMER");
            return roleRepository.save(role);
        });

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(customerRole));

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String token = jwtService.generateToken(userDetails, Map.of("roles", roles, "name", user.getFullName()));

        return new AuthResponse(
                token, "Bearer", jwtService.getExpirationMs(), user.getEmail(), user.getFullName(), roles);
    }
}
