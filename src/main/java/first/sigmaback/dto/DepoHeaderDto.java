package first.sigmaback.dto;

import lombok.Data;

@Data
public class DepoHeaderDto {
    private Long totalTransactionsInWork;
    private Integer planPppSum;
    private String totalOperationsWorkTimeSum;
    private int totalTimeBetweenOperationsHours;
    private int planPppDiff;
    private String planPppDiffPercentage;
    private int noOperationsCount;
}