package first.sigmaback.dto;

import lombok.Data;
import java.util.Map;

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
    private int totalHoursMounth;
    private int totalWorkTimeHoursFromEmployees; 
    private int totalTimeAllHours;
    private Double totalProblemHours;
    
    // Новые поля для диаграммы загрузки персонала
    private int totalEmployees;
    private int busyEmployees;
    private Map<String, Integer> employeesBySpecialization; // Всего по специальностям
    private Map<String, Integer> busyEmployeesBySpecialization; // Занято по специальностям
}