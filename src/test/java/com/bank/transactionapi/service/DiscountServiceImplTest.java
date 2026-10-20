package com.bank.transactionapi.service;

import com.bank.transactionapi.enums.CustomerType;
import com.bank.transactionapi.models.Customer;
import com.bank.transactionapi.strategy.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Slim context: real rules + real service, no datasource, no mocks
@SpringBootTest(classes = {
        DiscountServiceImpl.class,
        BusinessDiscountRule.class,
        RetailDiscountRule.class,
        LongTenureDiscountRule.class
})
class DiscountServiceImplTest {

    @Autowired
    private DiscountService discountService;

    // ---------- Retail rule (18%, above 50,000, from the 4th transaction) ----------

    @Test
    void retail_4thTransaction_above50000_gets18Percent() {
        assertThat(discount(retail(1), "60000", 3)).isEqualByComparingTo("10800.00");
    }

    @Test
    void retail_3rdTransaction_above50000_getsNothing() {
        assertThat(discount(retail(1), "60000", 2)).isEqualByComparingTo("0");
    }

    @Test
    void retail_exactly50000_getsNothing() {
        assertThat(discount(retail(1), "50000", 3)).isEqualByComparingTo("0");
    }

    @Test
    void retail_justAbove50000_getsDiscount() {
        assertThat(discount(retail(1), "50000.01", 3)).isEqualByComparingTo("9000.00");
    }

    @Test
    void discount_isRoundedToTwoDecimalsHalfUp() {
        // 60000.55 x 18% = 10800.099 -> 10800.10
        assertThat(discount(retail(1), "60000.55", 3)).isEqualByComparingTo("10800.10");
    }

    // ---------- Business rule (27%, above 150,000, from the 4th transaction) ----------

    @Test
    void business_4thTransaction_above150000_gets27Percent() {
        assertThat(discount(business(1), "200000", 3)).isEqualByComparingTo("54000.00");
    }

    @Test
    void business_2ndTransaction_above150000_getsNothing() {
        assertThat(discount(business(1), "200000", 1)).isEqualByComparingTo("0");
    }

    @Test
    void business_exactly150000_getsNothing() {
        assertThat(discount(business(1), "150000", 3)).isEqualByComparingTo("0");
    }

    @Test
    void business_doesNotGetTheRetailRule() {
        // 60,000 is above the retail threshold but below the business one
        assertThat(discount(business(1), "60000", 3)).isEqualByComparingTo("0");
    }

    // ---------- Long-tenure rule (10%, over 4 years, first three transactions) ----------

    @Test
    void longTenure_firstTransaction_gets10Percent() {
        assertThat(discount(retail(5), "10000", 0)).isEqualByComparingTo("1000.00");
    }

    @Test
    void longTenure_3rdTransaction_stillGets10Percent() {
        assertThat(discount(retail(5), "10000", 2)).isEqualByComparingTo("1000.00");
    }

    @Test
    void longTenure_4thTransaction_getsNothingFromThisRule() {
        assertThat(discount(retail(5), "10000", 3)).isEqualByComparingTo("0");
    }

    @Test
    void longTenure_exactlyFourYears_isNotOverFourYears() {
        Customer customer = customer(CustomerType.RETAIL, LocalDate.now().minusYears(4));
        assertThat(discount(customer, "10000", 0)).isEqualByComparingTo("0");
    }

    @Test
    void longTenure_fourYearsAndOneDay_qualifies() {
        Customer customer = customer(CustomerType.RETAIL, LocalDate.now().minusYears(4).minusDays(1));
        assertThat(discount(customer, "10000", 0)).isEqualByComparingTo("1000.00");
    }

    @Test
    void longTenureRetail_4thTransaction_above50000_getsRetailRule() {
        assertThat(discount(retail(5), "60000", 3)).isEqualByComparingTo("10800.00");
    }

    @Test
    void longTenureRetail_2ndTransaction_above50000_getsTenureRule() {
        assertThat(discount(retail(5), "60000", 1)).isEqualByComparingTo("6000.00");
    }

    // ---------- Decision 4: highest wins, no stacking; and Open/Closed ----------

    @Test
    void highestEligibleDiscountWins_noStacking() {
        DiscountService service = new DiscountServiceImpl(List.of(
                stub(true, "10"), stub(true, "27"), stub(true, "18")));

        // 27% only (not 10 + 27 + 18)
        assertThat(service.calculateDiscount(retail(1), new BigDecimal("100000"), 0))
                .isEqualByComparingTo("27000.00");
    }

    @Test
    void ineligibleRulesAreIgnored_evenIfTheirPercentageIsHigher() {
        DiscountService service = new DiscountServiceImpl(List.of(
                stub(false, "50"), stub(true, "10")));

        assertThat(service.calculateDiscount(retail(1), new BigDecimal("100000"), 0))
                .isEqualByComparingTo("10000.00");
    }

    @Test
    void aNewRuleIsJustANewClass_noExistingCodeChanges() {
        // a "fifth rule" added to the list, nothing else touched
        DiscountService service = new DiscountServiceImpl(List.of(
                new RetailDiscountRule(), new BusinessDiscountRule(), new LongTenureDiscountRule(),
                stub(true, "30")));

        assertThat(service.calculateDiscount(retail(1), new BigDecimal("1000"), 0))
                .isEqualByComparingTo("300.00");
    }

    @Test
    void noRuleApplies_discountIsZero() {
        DiscountService service = new DiscountServiceImpl(List.of(stub(false, "50")));

        assertThat(service.calculateDiscount(retail(1), new BigDecimal("1000"), 0))
                .isEqualByComparingTo("0");
    }

    // ---------- helpers ----------

    private BigDecimal discount(Customer customer, String amount, long alreadyThisMonth) {
        return discountService.calculateDiscount(customer, new BigDecimal(amount), alreadyThisMonth);
    }

    private Customer retail(int years) {
        return customer(CustomerType.RETAIL, LocalDate.now().minusYears(years).minusDays(1));
    }

    private Customer business(int years) {
        return customer(CustomerType.BUSINESS, LocalDate.now().minusYears(years).minusDays(1));
    }

    private Customer customer(CustomerType type, LocalDate dateJoined) {
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setName("Test Customer");
        customer.setCustomerType(type);
        customer.setDateJoined(dateJoined);
        return customer;
    }

    private static DiscountRule stub(boolean applicable, String percentage) {
        return new DiscountRule() {
            @Override
            public boolean isApplicable(DiscountContext context) {
                return applicable;
            }

            @Override
            public BigDecimal getDiscountPercentage() {
                return new BigDecimal(percentage);
            }
        };
    }
}