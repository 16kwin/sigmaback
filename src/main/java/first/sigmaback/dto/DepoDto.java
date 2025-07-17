package first.sigmaback.dto;

import lombok.Data;

@Data
public class DepoDto {
    private String transaction;
    private String vhodControlTimeExceeded;
    private String electricTimeExceeded;
    private String mechanicTimeExceeded;
    private String electronTimeExceeded;
    private String techTimeExceeded;
    private String vihodControlTimeExceeded;
    private String transportTimeExceeded;
}