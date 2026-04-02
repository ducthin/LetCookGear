package com.ducthin.LetCookGear.dto;

import com.ducthin.LetCookGear.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {

    @NotBlank
    private String receiverName;

    @NotBlank
    private String phone;

    @NotBlank
    private String province;

    @NotBlank
    private String district;

    @NotBlank
    private String ward;

    @NotBlank
    private String detail;

    @NotNull
    private PaymentMethod paymentMethod;
}
