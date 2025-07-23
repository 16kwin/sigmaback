package first.sigmaback.dto;

import lombok.Data;

import java.time.YearMonth;

@Data
public class MonthlyTransactionCountDto {
    private YearMonth month;
    private Long transactionCount;
}