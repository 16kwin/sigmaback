package first.sigmaback.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AnalisDTO {
    private String transaction;
    private String status;
    private Integer planPpp;
    private LocalDate planDateStart;
    private LocalDate forecastDateStart;
    private LocalDate factDateStart;
    private LocalDate planDateStop;
    private LocalDate forecastDateStop;
    private LocalDate factDateStop;
    private LocalDate planDateShipment;
    private LocalDate forecastDateShipment;
    private LocalDate factDateShipment;
    private String mechanicOptionWorktype;
    private String electronOptionWorktype;
    private String electricOptionWorktype;
    private String techOptionWorktype;
    private Double mechanicNorm;
    private Double electronNorm;
    private Double electricNorm;
    private Double techNorm;

    // Поля для TimeService
    private String vhodControlStartTime;
    private String vhodControlStopTime;
    private String vhodControlWorkTime;

    private String podkluchenieStartTime;
    private String podkluchenieStopTime;
    private String podkluchenieWorkTime;

    private String proverkaMehanikomStartTime;
    private String proverkaMehanikomStopTime;
    private String proverkaMehanikomWorkTime;

    private String proverkaElectronStartTime;
    private String proverkaElectronStopTime;
    private String proverkaElectronWorkTime;

    private String proverkaTehnologomStartTime;
    private String proverkaTehnologomStopTime;
    private String proverkaTehnologomWorkTime;

    private String vihodControlStartTime;
    private String vihodControlStopTime;
    private String vihodControlWorkTime;

    private String transportPolozhenieStartTime;
    private String transportPolozhenieStopTime;
    private String transportPolozhenieWorkTime;

    // Поля для суммарного времени
    private String mechanicTotalWorktime;
    private String electronTotalWorktime;
    private String electricTotalWorktime;
    private String techTotalWorktime;
}