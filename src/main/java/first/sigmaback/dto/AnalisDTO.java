package first.sigmaback.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AnalisDTO {
    private String transaction;
    private String status;
    private Integer planPpp;
    
    // ИЗМЕНЯЕМ LocalDate НА String
    private String planDateStart;
    private String forecastDateStart;
    private String factDateStart;
    private String planDateStop;
    private String forecastDateStop;
    private String factDateStop;
    private String planDateShipment;
    private String forecastDateShipment;
    private String factDateShipment;
    
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
    private String totalOperationsWorkTime;

    private Double mechanicProblemHours;
    private Double electronProblemHours;
    private Double electricProblemHours;
    private Double techProblemHours;
    private Double complexProblemHours;
    private Double totalProblemHours;

    private String vhodControlTimeExceeded;
    private String electricTimeExceeded;
    private String mechanicTimeExceeded;
    private String electronTimeExceeded;
    private String techTimeExceeded;
    private String TransportTimeExceeded;
    private String vihodControlTimeExceeded;

    // ИЗМЕНЯЕМ LocalDate НА String
    public String planDate1;
    public String planDate2;
    public String planDate3;
    public String planDate4;
    public String planDate5;
    public String planDate6;
    public String planDate7;
     
    // ИЗМЕНЯЕМ LocalDate НА String
    public String factDate1;
    public String factDate2;
    public String factDate3;
    public String factDate4;
    public String factDate5;
    public String factDate6;
    public String factDate7;

    private String vhodControlEmployee;
    private String podkluchenieEmployee;
    private String proverkaMehanikomEmployee;
    private String proverkaElectronEmployee;
    private String proverkaTehnologomEmployee;
    private String vihodControlEmployee;
    private String transportPolozhenieEmployee;

    private String timeBetweenVhodAndPodkluchenie;
    private String timeBetweenPodkluchenieAndProverkaMehanikom;
    private String timeBetweenProverkaMehanikomAndProverkaElectron;
    private String timeBetweenProverkaElectronAndProverkaTehnologom;
    private String timeBetweenProverkaTehnologomAndVihodControl;
    private String timeBetweenVihodControlAndTransportPolozhenie;
    private String totalTimeBetweenOperations;

    private String percentagePlanPpp;
    private String totalTimeAll;
    private double totalProfessionNorms;
}