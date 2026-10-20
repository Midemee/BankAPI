package com.bank.transactionapi.dtos;

import com.bank.transactionapi.enums.NetworkProvider;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class AirtimeRequest {

    @NotBlank(message = "sourceAccount is required")
    private String sourceAccount;

    @NotNull(message = "networkProvider is required (MTN, AIRTEL, GLO, ETISALAT)")
    private NetworkProvider networkProvider;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^(\\+234|0)[789][01]\\d{8}$",
            message = "phoneNumber must be a valid Nigerian number")
    private String phoneNumber;
}