package uk.gov.pay.ledger.report.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class PerformanceReportEntity {

    @JsonProperty("total_volume")
    private final long totalVolume;

    @JsonProperty("total_amount")
    private final BigDecimal totalAmount;

    @JsonProperty("average_amount")
    private final BigDecimal averageAmount;

    public PerformanceReportEntity(long totalVolume, BigDecimal totalAmount, BigDecimal averageAmount) {
        this.totalVolume = totalVolume;
        this.totalAmount = totalAmount;
        this.averageAmount = averageAmount;
    }

    public long getTotalVolume() {
        return totalVolume;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getAverageAmount() {
        return averageAmount;
    }
}
