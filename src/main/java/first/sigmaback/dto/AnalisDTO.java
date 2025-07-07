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

    public LocalDate planDate1;
    public LocalDate planDate2;
    public LocalDate planDate3;
    public LocalDate planDate4;
    public LocalDate planDate5;
    public LocalDate planDate6;
    public LocalDate planDate7;
     
     public LocalDate  factDate1;
    public LocalDate  factDate2;
    public LocalDate  factDate3;
    public LocalDate  factDate4;
    public LocalDate  factDate5;
    public LocalDate  factDate6;
    public LocalDate  factDate7;

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
}