package com.bank.transactionapi.service;

import com.bank.transactionapi.dtos.AirtimeRequest;
import com.bank.transactionapi.dtos.TransactionResponse;
import com.bank.transactionapi.dtos.TransferRequest;
import com.bank.transactionapi.enums.TransactionStatus;
import com.bank.transactionapi.enums.TransactionType;
import com.bank.transactionapi.exceptions.AccountNotFoundException;
import com.bank.transactionapi.exceptions.InvalidTransactionException;
import com.bank.transactionapi.mapper.TransactionMapper;
import com.bank.transactionapi.models.Account;
import com.bank.transactionapi.models.Airtime;
import com.bank.transactionapi.models.Customer;
import com.bank.transactionapi.models.Transaction;
import com.bank.transactionapi.models.Transfer;
import com.bank.transactionapi.repositories.AccountRepository;
import com.bank.transactionapi.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final DiscountService discountService;
    private final TransactionMapper transactionMapper;

    @Override
    public TransactionResponse transfer(TransferRequest request) {
        Account source = findAccount(request.getSourceAccount());
        Account destination = findAccount(request.getDestinationAccount());

        if (source.getAccountNumber().equals(destination.getAccountNumber())) {
            throw new InvalidTransactionException("Source and destination accounts must be different");
        }

        long countThisMonth = countTransactionsThisMonth(source.getCustomer());
        BigDecimal discount = discountService.calculateDiscount(
                source.getCustomer(), request.getAmount(), countThisMonth);

        Transfer transfer = new Transfer();
        transfer.setDestinationAccount(destination);
        populateCommonFields(transfer, source, request.getAmount(), discount, TransactionType.TRANSFER);

        return transactionMapper.toResponse(transactionRepository.save(transfer));
    }

    @Override
    public TransactionResponse buyAirtime(AirtimeRequest request) {
        Account source = findAccount(request.getSourceAccount());

        // Decision 3: airtime counts towards the monthly total (see countTransactionsThisMonth)
        // but never receives a discount, so the discount service is not called at all.
        Airtime airtime = new Airtime();
        airtime.setNetworkProvider(request.getNetworkProvider());
        airtime.setPhoneNumber(request.getPhoneNumber());
        populateCommonFields(airtime, source, request.getAmount(), BigDecimal.ZERO, TransactionType.AIRTIME);

        return transactionMapper.toResponse(transactionRepository.save(airtime));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
        if (!accountRepository.existsByAccountNumber(accountNumber)) {
            throw new AccountNotFoundException(accountNumber);
        }
        return transactionRepository
                .findBySourceAccountAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    // ---------- private helpers (one place for each repeated rule) ----------

    private Account findAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    // Decision 2: per customer, across all accounts, within the current calendar month
    private long countTransactionsThisMonth(Customer customer) {
        YearMonth month = YearMonth.now();
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        return transactionRepository.countByCustomerInPeriod(customer.getCustomerId(), start, end);
    }

    // Decision 5: store original, discount and final amount
    private void populateCommonFields(Transaction tx, Account source, BigDecimal amount,
                                      BigDecimal discount, TransactionType type) {
        tx.setSourceAccount(source);
        tx.setAmount(amount);
        tx.setDiscountAmount(discount);
        tx.calculateFinalAmount();
        tx.setTransactionType(type);
        tx.setStatus(TransactionStatus.SUCCESS);
    }
}