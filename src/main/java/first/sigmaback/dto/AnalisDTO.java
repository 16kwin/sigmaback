package first.sigmaback.dto;

import java.time.LocalDate;

import lombok.Data;

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
}
