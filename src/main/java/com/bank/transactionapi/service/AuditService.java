package com.bank.transactionapi.service;

public interface AuditService {

    void record(String httpMethod, String endpoint, int statusCode, String errorMessage);
}