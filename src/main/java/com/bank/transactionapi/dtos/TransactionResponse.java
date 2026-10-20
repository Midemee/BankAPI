package com.bank.transactionapi.dtos;

import com.bank.transactionapi.enums.NetworkProvider;
import com.bank.transactionapi.enums.TransactionStatus;
import com.bank.transactionapi.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TransactionResponse {

    private Long transactionId;
    private TransactionType transactionType;
    private TransactionStatus status;

    private String sourceAccount;
    private String destinationAccount;
    private NetworkProvider networkProvider;
    private String phoneNumber;

    private BigDecimal amount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    private LocalDateTime createdAt;
}