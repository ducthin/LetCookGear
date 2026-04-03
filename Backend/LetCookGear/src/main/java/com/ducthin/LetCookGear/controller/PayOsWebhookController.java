package com.ducthin.LetCookGear.controller;

import com.ducthin.LetCookGear.dto.ApiResponse;
import com.ducthin.LetCookGear.payment.PayOsWebhookService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/payos")
@RequiredArgsConstructor
public class PayOsWebhookController {

    private final PayOsWebhookService payOsWebhookService;

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> receiveWebhook(@RequestBody Map<String, Object> payload) {
        boolean paid = payOsWebhookService.handleWebhook(payload);
        String message = paid
                ? "Đã ghi nhận thanh toán PayOS thành công"
                : "Đã ghi nhận webhook PayOS";

        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
