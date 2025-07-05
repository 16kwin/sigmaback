package first.sigmaback.dto;

import lombok.Data;

@Data
public class AnalisHeaderDTO {
    private String vhodNorm;  // Норматив для "Входного контроля"
    private String podklyuchenieNorm;
    private String mechOperationNorm;   // Норматив для "Подключения"
    private String electronOperationNorm;
    private String techOperationNorm;
    private String vihodNorm;
    private String transportNorm;
    private Integer mechanicCount;
    private Integer eletronCount;
    private Integer techCount;
    private Integer elecCount;
    private Integer conplectCount;
}