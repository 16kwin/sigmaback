package first.sigmaback.dto;

import lombok.Data;

@Data
public class AnalisHeaderDTO {
    private String vhodNorm;
    private String podklyuchenieNorm;
    private String mechOperationNorm;
    private String electronOperationNorm;
    private String techOperationNorm;
    private String vihodNorm;
    private String transportNorm;
    private double totalHeaderNorms;
    
    // Всего сотрудников по специальностям
    private Integer mechanicCount;
    private Integer eletronCount;
    private Integer techCount;
    private Integer elecCount;
    private Integer conplectCount;
    
    // Занято сотрудников по специальностям
    private Integer mechanicBusy;
    private Integer eletronBusy;
    private Integer techBusy;
    private Integer elecBusy;
    private Integer conplectBusy;
    
    // Свободно сотрудников по специальностям
    private Integer mechanicFree;
    private Integer eletronFree;
    private Integer techFree;
    private Integer elecFree;
    private Integer conplectFree;

    // Статистика по транзакциям
    private long inProgressTransactionsCount;
    private long overfulfilledTransactionsCount;
    private long underfulfilledTransactionsCount;
}