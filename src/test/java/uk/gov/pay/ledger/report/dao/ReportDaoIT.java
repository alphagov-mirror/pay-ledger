package uk.gov.pay.ledger.report.dao;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.report.entity.PaymentCountByStateResult;
import uk.gov.pay.ledger.report.entity.PaymentsStatisticsResult;
import uk.gov.pay.ledger.report.params.PaymentsReportParams;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class ReportDaoIT {
    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private ReportDao reportDao;

    private DatabaseTestHelper databaseTestHelper = aDatabaseTestHelper(rule.getJdbi());

    @Before
    public void setUp() {
        databaseTestHelper.truncateAllData();
        reportDao = new ReportDao(rule.getJdbi());
    }

    @Test
    public void shouldReturnCountsForStatuses_whenSearchingWithNoParameters() {
        aTransactionFixture().withState(TransactionState.CREATED).insert(rule.getJdbi());
        aTransactionFixture().withState(TransactionState.CREATED).insert(rule.getJdbi());
        aTransactionFixture().withState(TransactionState.ERROR).insert(rule.getJdbi());
        aTransactionFixture().withState(TransactionState.SUCCESS).insert(rule.getJdbi());

        var params = new PaymentsReportParams();

        List<PaymentCountByStateResult> paymentCountsByState = reportDao.getPaymentCountsByState(params);

        assertThat(paymentCountsByState, hasSize(3));
        assertThat(paymentCountsByState, containsInAnyOrder(
                is(new PaymentCountByStateResult("CREATED", 2L)),
                is(new PaymentCountByStateResult("ERROR", 1L)),
                is(new PaymentCountByStateResult("SUCCESS", 1L))
        ));
    }

    @Test
    public void shouldReturnCountsForStatuses_whenSearchingByGatewayAccount() {
        String gatewayAccountId1 = "account-1";
        String gatewayAccountId2 = "account-2";

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withState(TransactionState.CREATED)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId2)
                .withState(TransactionState.CREATED)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withState(TransactionState.ERROR)
                .insert(rule.getJdbi());

        var params = new PaymentsReportParams();
        params.setAccountId(gatewayAccountId1);

        List<PaymentCountByStateResult> paymentCountsByState = reportDao.getPaymentCountsByState(params);

        assertThat(paymentCountsByState, hasSize(2));
        assertThat(paymentCountsByState, containsInAnyOrder(
                is(new PaymentCountByStateResult("CREATED", 1L)),
                is(new PaymentCountByStateResult("ERROR", 1L))
        ));
    }

    @Test
    public void shouldReturnCountsForStatuses_whenSearchingByFromDateAndToDate() {
        aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-09-29T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withState(TransactionState.ERROR)
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withState(TransactionState.SUCCESS)
                .withCreatedDate(ZonedDateTime.parse("2019-09-29T00:00:00.000Z"))
                .insert(rule.getJdbi());

        var params = new PaymentsReportParams();
        params.setFromDate("2019-09-29T23:59:59.000Z");
        params.setToDate("2019-10-01T00:00:00.000Z");

        List<PaymentCountByStateResult> paymentCountsByState = reportDao.getPaymentCountsByState(params);

        assertThat(paymentCountsByState, hasSize(2));
        assertThat(paymentCountsByState, containsInAnyOrder(
                is(new PaymentCountByStateResult("CREATED", 1L)),
                is(new PaymentCountByStateResult("ERROR", 1L))
        ));
    }

    @Test
    public void shouldReturnCountsForStatuses_whenSearchingByAllParameters() {
        String gatewayAccountId1 = "account-1";
        String gatewayAccountId2 = "account-2";

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-09-29T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId2)
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withState(TransactionState.ERROR)
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .insert(rule.getJdbi());

        var params = new PaymentsReportParams();
        params.setAccountId(gatewayAccountId1);
        params.setFromDate("2019-09-29T23:59:59.000Z");
        params.setToDate("2019-10-01T00:00:00.000Z");

        List<PaymentCountByStateResult> paymentCountsByState = reportDao.getPaymentCountsByState(params);

        assertThat(paymentCountsByState, hasSize(2));
        assertThat(paymentCountsByState, containsInAnyOrder(
                is(new PaymentCountByStateResult("CREATED", 1L)),
                is(new PaymentCountByStateResult("ERROR", 1L))
        ));
    }

    @Test
    public void shouldReturnPaymentsStatisticsForSuccessfulPayments_whenSearchingWithNoParameters() {
        aTransactionFixture()
                .withTotalAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withTotalAmount(2000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withTotalAmount(3000L)
                .withState(TransactionState.CREATED)
                .insert(rule.getJdbi());

        var params = new PaymentsReportParams();

        PaymentsStatisticsResult paymentsStatistics = reportDao.getPaymentsStatistics(params);

        assertThat(paymentsStatistics.getCount(), is(2L));
        assertThat(paymentsStatistics.getGrossAmount(), is(3000L));
    }

    @Test
    public void shouldReturnPaymentsStatistics_whenSearchingByGatewayAccount() {
        String gatewayAccountId1 = "account-1";
        String gatewayAccountId2 = "account-2";

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withTotalAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withTotalAmount(2000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId2)
                .withTotalAmount(3000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());

        var params = new PaymentsReportParams();
        params.setAccountId(gatewayAccountId1);

        PaymentsStatisticsResult paymentsStatistics = reportDao.getPaymentsStatistics(params);

        assertThat(paymentsStatistics.getCount(), is(2L));
        assertThat(paymentsStatistics.getGrossAmount(), is(3000L));
    }

    @Test
    public void shouldReturnPaymentsStatistics_whenSearchingByFromDateAndToDate() {

        aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T00:00:00.000Z"))
                .withTotalAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withTotalAmount(2000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withTotalAmount(3000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse("2019-09-29T00:00:00.000Z"))
                .withTotalAmount(4000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());

        var params = new PaymentsReportParams();
        params.setFromDate("2019-09-29T23:59:59.000Z");
        params.setToDate("2019-10-01T00:00:00.000Z");

        PaymentsStatisticsResult paymentsStatistics = reportDao.getPaymentsStatistics(params);

        assertThat(paymentsStatistics.getCount(), is(2L));
        assertThat(paymentsStatistics.getGrossAmount(), is(5000L));
    }
}