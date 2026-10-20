package com.bank.transactionapi.service;

import com.bank.transactionapi.dtos.AirtimeRequest;
import com.bank.transactionapi.dtos.TransactionResponse;
import com.bank.transactionapi.dtos.TransferRequest;

import java.util.List;

public interface TransactionService {

    TransactionResponse transfer(TransferRequest request);

    TransactionResponse buyAirtime(AirtimeRequest request);

    List<TransactionResponse> getTransactionHistory(String accountNumber);
}