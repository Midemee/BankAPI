package com.bank.transactionapi.service;

import com.bank.transactionapi.dtos.AirtimeRequest;
import com.bank.transactionapi.dtos.TransactionResponse;
import com.bank.transactionapi.dtos.TransferRequest;
import com.bank.transactionapi.enums.CustomerType;
import com.bank.transactionapi.enums.NetworkProvider;
import com.bank.transactionapi.exceptions.AccountNotFoundException;
import com.bank.transactionapi.exceptions.InvalidTransactionException;
import com.bank.transactionapi.models.Account;
import com.bank.transactionapi.models.Customer;
import com.bank.transactionapi.repositories.AccountRepository;
import com.bank.transactionapi.repositories.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionServiceIntegrationTest {

    private static final String SOURCE = "1111111111";
    private static final String SECOND_ACCOUNT = "2222222222";
    private static final String DESTINATION = "3333333333";

    @Autowired private TransactionService transactionService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccountRepository accountRepository;

    @Test
    void retailCustomer_4thTransfer_60000_gets18Percent() {
        givenCustomer(CustomerType.RETAIL, 1);
        seedTransfers(3);

        TransactionResponse response = transfer(SOURCE, "60000");

        assertThat(response.getDiscountAmount()).isEqualByComparingTo("10800.00");
        assertThat(response.getFinalAmount()).isEqualByComparingTo("49200.00");
    }

    @Test
    void businessCustomer_2ndTransfer_200000_getsNoDiscount() {
        givenCustomer(CustomerType.BUSINESS, 1);
        seedTransfers(1);

        TransactionResponse response = transfer(SOURCE, "200000");

        assertThat(response.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(response.getFinalAmount()).isEqualByComparingTo("200000");
    }

    @Test
    void businessCustomer_5thTransfer_120000_getsNoDiscount() {
        givenCustomer(CustomerType.BUSINESS, 1);
        seedTransfers(4);

        TransactionResponse response = transfer(SOURCE, "120000");

        assertThat(response.getDiscountAmount()).isEqualByComparingTo("0");
    }

    @Test
    void fiveYearCustomer_1stTransfer_10000_gets10Percent() {
        givenCustomer(CustomerType.RETAIL, 5);

        TransactionResponse response = transfer(SOURCE, "10000");

        assertThat(response.getDiscountAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.getFinalAmount()).isEqualByComparingTo("9000.00");
    }

    @Test
    void fiveYearCustomer_1stAirtime_5000_neverDiscounted() {
        givenCustomer(CustomerType.RETAIL, 5);

        TransactionResponse response = airtime("5000");

        assertThat(response.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(response.getFinalAmount()).isEqualByComparingTo("5000");
    }

    @Test
    void decision1_discountDoesNotApplyOn3rdTransaction_butAppliesOn4th() {
        givenCustomer(CustomerType.RETAIL, 1);
        seedTransfers(2);

        assertThat(transfer(SOURCE, "60000").getDiscountAmount()).isEqualByComparingTo("0");     // 3rd
        assertThat(transfer(SOURCE, "60000").getDiscountAmount()).isEqualByComparingTo("10800"); // 4th
    }

    @Test
    void decision2_monthlyCountIsPerCustomerAcrossAllAccounts() {
        givenCustomer(CustomerType.RETAIL, 1);
        transfer(SOURCE, "1000");
        transfer(SECOND_ACCOUNT, "1000");
        transfer(SOURCE, "1000");

        TransactionResponse response = transfer(SECOND_ACCOUNT, "60000");

        assertThat(response.getDiscountAmount()).isEqualByComparingTo("10800.00");
    }

    @Test
    void decision3_airtimeCountsTowardsTheThree() {
        givenCustomer(CustomerType.RETAIL, 1);
        transfer(SOURCE, "1000");
        transfer(SOURCE, "1000");
        airtime("1000");

        TransactionResponse response = transfer(SOURCE, "60000");   // 4th overall

        assertThat(response.getDiscountAmount()).isEqualByComparingTo("10800.00");
    }

    @Test
    void businessCustomer_4thTransfer_200000_gets27Percent() {
        givenCustomer(CustomerType.BUSINESS, 1);
        seedTransfers(3);

        TransactionResponse response = transfer(SOURCE, "200000");

        assertThat(response.getDiscountAmount()).isEqualByComparingTo("54000.00");
        assertThat(response.getFinalAmount()).isEqualByComparingTo("146000.00");
    }

    @Test
    void decision5_originalDiscountAndFinalAmountAreAllStored() {
        givenCustomer(CustomerType.RETAIL, 1);
        seedTransfers(3);

        TransactionResponse response = transfer(SOURCE, "60000");

        assertThat(response.getAmount()).isEqualByComparingTo("60000");
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("10800.00");
        assertThat(response.getFinalAmount()).isEqualByComparingTo("49200.00");
    }

    @Test
    void history_returnsTransactionsForThatAccountOnly() {
        givenCustomer(CustomerType.RETAIL, 1);
        transfer(SOURCE, "1000");
        transfer(SOURCE, "2000");
        transfer(SECOND_ACCOUNT, "3000");

        assertThat(transactionService.getTransactionHistory(SOURCE)).hasSize(2);
        assertThat(transactionService.getTransactionHistory(SECOND_ACCOUNT)).hasSize(1);
    }

    @Test
    void history_unknownAccount_throwsAccountNotFound() {
        assertThatThrownBy(() -> transactionService.getTransactionHistory("9999999999"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void transfer_unknownSourceAccount_throwsAccountNotFound() {
        givenCustomer(CustomerType.RETAIL, 1);

        assertThatThrownBy(() -> transfer("9999999999", "1000"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void transfer_sameSourceAndDestination_throwsInvalidTransaction() {
        givenCustomer(CustomerType.RETAIL, 1);

        TransferRequest request = transferRequest(SOURCE, "1000");
        request.setDestinationAccount(SOURCE);

        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(InvalidTransactionException.class);
    }

    private void givenCustomer(CustomerType type, int yearsAsCustomer) {
        Customer customer = new Customer();
        customer.setName("Test Customer");
        customer.setCustomerType(type);
        customer.setDateJoined(LocalDate.now().minusYears(yearsAsCustomer).minusDays(1));
        customer = customerRepository.save(customer);

        saveAccount(SOURCE, customer);
        saveAccount(SECOND_ACCOUNT, customer);
        saveAccount(DESTINATION, customer);
    }

    private void saveAccount(String number, Customer customer) {
        Account account = new Account();
        account.setAccountNumber(number);
        account.setCustomer(customer);
        accountRepository.save(account);
    }

    private void seedTransfers(int count) {
        for (int i = 0; i < count; i++) {
            transfer(SOURCE, "1000");
        }
    }

    private TransactionResponse transfer(String sourceAccount, String amount) {
        return transactionService.transfer(transferRequest(sourceAccount, amount));
    }

    private TransferRequest transferRequest(String sourceAccount, String amount) {
        TransferRequest request = new TransferRequest();
        request.setSourceAccount(sourceAccount);
        request.setDestinationAccount(DESTINATION);
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private TransactionResponse airtime(String amount) {
        AirtimeRequest request = new AirtimeRequest();
        request.setSourceAccount(SOURCE);
        request.setNetworkProvider(NetworkProvider.MTN);
        request.setAmount(new BigDecimal(amount));
        request.setPhoneNumber("08031234567");
        return transactionService.buyAirtime(request);
    }
}