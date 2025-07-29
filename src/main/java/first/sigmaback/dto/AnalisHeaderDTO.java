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
    private Integer mechanicCount;
    private Integer eletronCount;
    private Integer techCount;
    private Integer elecCount;
    private Integer conplectCount;

    // Статистика по транзакциям
    private long inProgressTransactionsCount;
    private long overfulfilledTransactionsCount;
    private long underfulfilledTransactionsCount;
   

}