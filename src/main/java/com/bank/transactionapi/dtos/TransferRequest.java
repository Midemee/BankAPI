package com.bank.transactionapi.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class TransferRequest {
    @NotBlank(message = "sourceAccount is required")
    private String sourceAccount;

    @NotBlank(message = "destinationAccount is required")
    private String destinationAccount;

    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    private BigDecimal amount;


}
