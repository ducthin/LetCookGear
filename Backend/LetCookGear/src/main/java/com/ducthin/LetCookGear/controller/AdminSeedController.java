package com.ducthin.LetCookGear.controller;

import com.ducthin.LetCookGear.config.DevDataSeeder;
import com.ducthin.LetCookGear.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/seed")
@RequiredArgsConstructor
public class AdminSeedController {

    private final DevDataSeeder devDataSeeder;

    @Value("${app.seed.endpoint-enabled:false}")
    private boolean seedEndpointEnabled;

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<Void>> runSeed() {
        if (!seedEndpointEnabled) {
            throw new IllegalArgumentException("Seed endpoint is disabled by configuration");
        }

        devDataSeeder.seedData();
        return ResponseEntity.ok(ApiResponse.success("Seed completed"));
    }
}
