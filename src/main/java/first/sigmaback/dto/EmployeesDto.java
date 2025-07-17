package first.sigmaback.dto;

import lombok.Data;

@Data
public class EmployeesDto {
    private String employeeName;
     private String employeeSpecialization;
     private int transactionCount;
      private int exceededTimeCount;
      private String exceededOrNoOperations;
       private int totalNormTime;
        private String totalWorkTime; 
        private String workTimePercentage;
        private Integer hoursMounth;
        private String hoursMounthPercentage;
}