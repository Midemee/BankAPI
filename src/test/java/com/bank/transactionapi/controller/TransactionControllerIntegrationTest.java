package com.bank.transactionapi.controller;

import com.bank.transactionapi.audit.AuditLog;
import com.bank.transactionapi.enums.CustomerType;
import com.bank.transactionapi.models.Account;
import com.bank.transactionapi.models.Customer;
import com.bank.transactionapi.repositories.AccountRepository;
import com.bank.transactionapi.repositories.AuditLogRepository;
import com.bank.transactionapi.repositories.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionControllerIntegrationTest {

    private static final String TRANSFER_URL = "/api/v1/transactions/transfer";
    private static final String AIRTIME_URL = "/api/v1/transactions/airtime";

    @Autowired private WebApplicationContext context;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        Customer customer = new Customer();
        customer.setName("Test Customer");
        customer.setCustomerType(CustomerType.RETAIL);
        customer.setDateJoined(LocalDate.now().minusYears(1));
        customer = customerRepository.save(customer);

        saveAccount("1111111111", customer);
        saveAccount("2222222222", customer);
    }

    @Test
    void transfer_validRequest_returns201() throws Exception {
        mockMvc.perform(post(TRANSFER_URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"sourceAccount":"1111111111","destinationAccount":"2222222222","amount":5000}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.sourceAccount").value("1111111111"))
                .andExpect(jsonPath("$.destinationAccount").value("2222222222"))
                .andExpect(jsonPath("$.finalAmount").isNumber());
    }

    @Test
    void transfer_missingFields_returns400WithDetails() throws Exception {
        mockMvc.perform(post(TRANSFER_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void transfer_negativeAmount_returns400() throws Exception {
        mockMvc.perform(post(TRANSFER_URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"sourceAccount":"1111111111","destinationAccount":"2222222222","amount":-5}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_unknownAccount_returns404() throws Exception {
        mockMvc.perform(post(TRANSFER_URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"sourceAccount":"9999999999","destinationAccount":"2222222222","amount":5000}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found: 9999999999"));
    }

    @Test
    void transfer_toSameAccount_returns400() throws Exception {
        mockMvc.perform(post(TRANSFER_URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"sourceAccount":"1111111111","destinationAccount":"1111111111","amount":5000}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void airtime_validRequest_returns201_withNoDiscount() throws Exception {
        mockMvc.perform(post(AIRTIME_URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"sourceAccount":"1111111111","networkProvider":"MTN","amount":2000,"phoneNumber":"08031234567"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("AIRTIME"))
                .andExpect(jsonPath("$.networkProvider").value("MTN"))
                .andExpect(jsonPath("$.phoneNumber").value("08031234567"));
    }

    @Test
    void airtime_invalidPhoneNumber_returns400() throws Exception {
        mockMvc.perform(post(AIRTIME_URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"sourceAccount":"1111111111","networkProvider":"MTN","amount":2000,"phoneNumber":"123"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void airtime_unknownNetworkProvider_returns400() throws Exception {
        mockMvc.perform(post(AIRTIME_URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"sourceAccount":"1111111111","networkProvider":"VODAFONE","amount":2000,"phoneNumber":"08031234567"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void history_returnsTransactionsForTheAccount() throws Exception {
        transferOnce(1000);
        transferOnce(2000);

        mockMvc.perform(get("/api/v1/accounts/1111111111/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void history_unknownAccount_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/9999999999/transactions"))
                .andExpect(status().isNotFound());
    }

    @Test
    void successfulRequest_isAudited() throws Exception {
        transferOnce(1000);

        AuditLog entry = onlyAuditEntry();
        assertThat(entry.getHttpMethod()).isEqualTo("POST");
        assertThat(entry.getEndpoint()).isEqualTo(TRANSFER_URL);
        assertThat(entry.getStatusCode()).isEqualTo(201);
        assertThat(entry.isSuccess()).isTrue();
    }

    @Test
    void failedRequest_isAuditedWithTheErrorMessage() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/9999999999/transactions"));

        AuditLog entry = onlyAuditEntry();
        assertThat(entry.getHttpMethod()).isEqualTo("GET");
        assertThat(entry.getStatusCode()).isEqualTo(404);
        assertThat(entry.isSuccess()).isFalse();
        assertThat(entry.getErrorMessage()).contains("Account not found");
    }

    @Test
    void validationFailure_isAudited() throws Exception {
        mockMvc.perform(post(TRANSFER_URL).contentType(MediaType.APPLICATION_JSON).content("{}"));

        AuditLog entry = onlyAuditEntry();
        assertThat(entry.getStatusCode()).isEqualTo(400);
        assertThat(entry.isSuccess()).isFalse();
    }

    private void transferOnce(int amount) throws Exception {
        mockMvc.perform(post(TRANSFER_URL).contentType(MediaType.APPLICATION_JSON).content(
                "{\"sourceAccount\":\"1111111111\",\"destinationAccount\":\"2222222222\",\"amount\":" + amount + "}"));
    }

    private AuditLog onlyAuditEntry() {
        List<AuditLog> all = auditLogRepository.findAll();
        assertThat(all).hasSize(1);
        return all.get(0);
    }

    private void saveAccount(String number, Customer customer) {
        Account account = new Account();
        account.setAccountNumber(number);
        account.setCustomer(customer);
        accountRepository.save(account);
    }
}