package uk.gov.pay.ledger.report.dao;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.report.entity.GatewayAccountMonthlyPerformanceReportEntity;
import uk.gov.pay.ledger.report.params.PerformanceReportParams.PerformanceReportParamsBuilder;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.math.BigDecimal.ZERO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class PerformanceReportDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private DatabaseTestHelper databaseTestHelper = aDatabaseTestHelper(rule.getJdbi());

    private PerformanceReportDao transactionDao;

    @BeforeEach
    public void setUp() {
        databaseTestHelper.truncateAllData();
        transactionDao = new PerformanceReportDao(rule.getJdbi());
    }

    @Test
    public void report_volume_total_amount_and_average_amount_for_date_range() {
        Stream.of("2019-12-31T10:00:00Z", "2019-12-30T10:00:00Z", "2017-11-29T10:00:00Z").forEach(time -> aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse(time))
                .withAmount(100L)
                .withState(TransactionState.SUCCESS)
                .withTransactionType("PAYMENT")
                .withLive(true)
                .insert(rule.getJdbi()));

        Stream.of("2019-12-12T10:00:00Z", "2019-12-11T10:00:00Z", "2017-11-30T10:00:00Z").forEach(time -> aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse(time))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withTransactionType("PAYMENT")
                .withLive(true)
                .insert(rule.getJdbi()));

        var performanceReportParams = PerformanceReportParamsBuilder.builder()
                .withFromDate(ZonedDateTime.parse("2017-11-30T10:00:00Z"))
                .withToDate(ZonedDateTime.parse("2019-12-12T10:00:00Z"))
                .build();
        var performanceReportEntity = transactionDao.performanceReportForPaymentTransactions(performanceReportParams);
        assertThat(performanceReportEntity.getTotalVolume(), is(3L));
        assertThat(performanceReportEntity.getTotalAmount(), is(closeTo(new BigDecimal(3000L), ZERO)));
        assertThat(performanceReportEntity.getAverageAmount(), is(closeTo(new BigDecimal(1000L), ZERO)));
    }

    @Test
    public void report_volume_total_amount_and_average_amount_by_state() {
        aTransactionFixture().withAmount(1000L).withState(TransactionState.STARTED).withTransactionType("PAYMENT")
                .withLive(true).insert(rule.getJdbi());

        aTransactionFixture().withAmount(1000L).withState(TransactionState.SUCCESS).withTransactionType("REFUND")
                .withLive(true).insert(rule.getJdbi());

        aTransactionFixture().withAmount(1000L).withState(TransactionState.SUCCESS).withTransactionType("PAYMENT")
                .withLive(false).insert(rule.getJdbi());

        List<Long> relevantAmounts = List.of(1200L, 1020L, 750L);
        relevantAmounts.forEach(amount -> aTransactionFixture()
                .withAmount(amount)
                .withState(TransactionState.SUCCESS)
                .withTransactionType("PAYMENT")
                .withLive(true)
                .insert(rule.getJdbi()));
        BigDecimal expectedTotalAmount = new BigDecimal(relevantAmounts.stream().mapToLong(amount -> amount).sum());
        BigDecimal expectedAverageAmount = BigDecimal.valueOf(relevantAmounts.stream().mapToLong(amount -> amount).average().getAsDouble());

        var performanceReportParams = PerformanceReportParamsBuilder.builder().withState(TransactionState.SUCCESS).build();
        var performanceReportEntity = transactionDao.performanceReportForPaymentTransactions(performanceReportParams);
        assertThat(performanceReportEntity.getTotalVolume(), is(3L));
        assertThat(performanceReportEntity.getTotalAmount(), is(closeTo(expectedTotalAmount, ZERO)));
        assertThat(performanceReportEntity.getAverageAmount(), is(closeTo(expectedAverageAmount, ZERO)));
    }

    @Test
    public void report_volume_total_amount_and_average_amount_for_all_payments() {
        Stream.of(TransactionState.STARTED, TransactionState.SUCCESS, TransactionState.SUBMITTED).forEach(state ->
                aTransactionFixture()
                        .withAmount(1000L)
                        .withState(state)
                        .withTransactionType("PAYMENT")
                        .withLive(true)
                        .insert(rule.getJdbi()));

        var performanceReportParams = PerformanceReportParamsBuilder.builder().build();
        var performanceReportEntity = transactionDao.performanceReportForPaymentTransactions(performanceReportParams);
        assertThat(performanceReportEntity.getTotalVolume(), is(3L));
        assertThat(performanceReportEntity.getTotalAmount(), is(closeTo(new BigDecimal(3000L), ZERO)));
        assertThat(performanceReportEntity.getAverageAmount(), is(closeTo(new BigDecimal(1000L), ZERO)));
    }

    @Test
    public void verifyMonthlyGatewayPerformanceReportTest() {
        aTransactionFixture()
                .withGatewayAccountId("1")
                .withAmount(1000L)
                .withState(TransactionState.STARTED)
                .withTransactionType("PAYMENT")
                .withLive(true)
                .withCreatedDate(ZonedDateTime.parse("2019-01-01T02:00:00Z"))
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId("1")
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withTransactionType("REFUND")
                .withLive(true)
                .withCreatedDate(ZonedDateTime.parse("2019-01-01T02:00:00Z"))
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId("1")
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withTransactionType("PAYMENT")
                .withLive(true)
                .withCreatedDate(ZonedDateTime.parse("2018-01-01T02:00:00Z"))
                .insert(rule.getJdbi());

        List<String> relevantGatewayAccounts = List.of("1", "2");
        relevantGatewayAccounts.forEach(account -> aTransactionFixture()
                .withGatewayAccountId(account)
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withTransactionType("PAYMENT")
                .withLive(true)
                .withCreatedDate(ZonedDateTime.parse("2019-01-01T02:00:00Z"))
                .insert(rule.getJdbi()));

        BigDecimal expectedValue = BigDecimal.valueOf(1000);
        ZonedDateTime startDate = ZonedDateTime.parse("2019-01-01T00:00:00Z");
        ZonedDateTime endDate = ZonedDateTime.parse("2019-02-01T00:00:00Z");

        List<GatewayAccountMonthlyPerformanceReportEntity> performanceReport = transactionDao.monthlyPerformanceReportForGatewayAccounts(startDate, endDate);

        GatewayAccountMonthlyPerformanceReportEntity gatewayAccountOnePerformanceReport = performanceReport.get(0);
        GatewayAccountMonthlyPerformanceReportEntity gatewayAccountTwoPerformanceReport = performanceReport.get(1);

        assertThat(performanceReport.size(), is(2));

        assertThat(gatewayAccountOnePerformanceReport.getGatewayAccountId(), is(1L));
        assertThat(gatewayAccountOnePerformanceReport.getTotalVolume(), is(1L));
        assertThat(gatewayAccountOnePerformanceReport.getTotalAmount(), is(closeTo(expectedValue, ZERO)));
        assertThat(gatewayAccountOnePerformanceReport.getAverageAmount(), is(closeTo(expectedValue, ZERO)));
        assertThat(gatewayAccountOnePerformanceReport.getMinimumAmount(), is(closeTo(expectedValue, ZERO)));
        assertThat(gatewayAccountOnePerformanceReport.getMaximumAmount(), is(closeTo(expectedValue, ZERO)));
        assertThat(gatewayAccountOnePerformanceReport.getYear(), is(2019L));
        assertThat(gatewayAccountOnePerformanceReport.getMonth(), is(1L));

        assertThat(gatewayAccountTwoPerformanceReport.getGatewayAccountId(), is(2L));
        assertThat(gatewayAccountTwoPerformanceReport.getTotalVolume(), is(1L));
        assertThat(gatewayAccountTwoPerformanceReport.getTotalAmount(), is(closeTo(expectedValue, ZERO)));
        assertThat(gatewayAccountTwoPerformanceReport.getAverageAmount(), is(closeTo(expectedValue, ZERO)));
        assertThat(gatewayAccountTwoPerformanceReport.getMinimumAmount(), is(closeTo(expectedValue, ZERO)));
        assertThat(gatewayAccountTwoPerformanceReport.getMaximumAmount(), is(closeTo(expectedValue, ZERO)));
        assertThat(gatewayAccountTwoPerformanceReport.getYear(), is(2019L));
        assertThat(gatewayAccountTwoPerformanceReport.getMonth(), is(1L));
    }
}
