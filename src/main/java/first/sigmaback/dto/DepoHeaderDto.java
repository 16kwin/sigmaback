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
    private int yesOperationsCount;
    private int vhodControlExceededCount;
    private int electricExceededCount;
    private int mechanicExceededCount;
    private int electronExceededCount;
    private int techExceededCount;
    private int vihodControlExceededCount;
    private int transportExceededCount;
     private int totalHoursMounth; // Новое поле
    private int totalWorkTimeHoursFromEmployees; 
    private int totalTimeAllHours;
     private Double totalProblemHours;
}