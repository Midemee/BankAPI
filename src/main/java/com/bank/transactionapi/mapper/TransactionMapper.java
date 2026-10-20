package com.bank.transactionapi.mapper;

import com.bank.transactionapi.dtos.TransactionResponse;
import com.bank.transactionapi.models.Airtime;
import com.bank.transactionapi.models.Transaction;
import com.bank.transactionapi.models.Transfer;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction tx) {
        TransactionResponse.TransactionResponseBuilder builder = TransactionResponse.builder()
                .transactionId(tx.getTransactionId())
                .transactionType(tx.getTransactionType())
                .status(tx.getStatus())
                .sourceAccount(tx.getSourceAccount().getAccountNumber())
                .amount(tx.getAmount())
                .discountAmount(tx.getDiscountAmount())
                .finalAmount(tx.getFinalAmount())
                .createdAt(tx.getCreatedAt());

        if (tx instanceof Transfer transfer) {
            builder.destinationAccount(transfer.getDestinationAccount().getAccountNumber());
        } else if (tx instanceof Airtime airtime) {
            builder.networkProvider(airtime.getNetworkProvider())
                    .phoneNumber(airtime.getPhoneNumber());
        }
        return builder.build();
    }
}