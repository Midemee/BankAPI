package com.bank.transactionapi.controller;

import com.bank.transactionapi.dtos.AirtimeRequest;
import com.bank.transactionapi.dtos.TransactionResponse;
import com.bank.transactionapi.dtos.TransferRequest;
import com.bank.transactionapi.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transactions/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transfer(request));
    }

    @PostMapping("/transactions/airtime")
    public ResponseEntity<TransactionResponse> buyAirtime(@Valid @RequestBody AirtimeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.buyAirtime(request));
    }

    @GetMapping("/accounts/{accountNumber}/transactions")
    public List<TransactionResponse> getTransactionHistory(@PathVariable String accountNumber) {
        return transactionService.getTransactionHistory(accountNumber);
    }
}