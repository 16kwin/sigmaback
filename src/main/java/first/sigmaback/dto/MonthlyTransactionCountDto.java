package first.sigmaback.dto;

import lombok.Data;
import java.time.YearMonth;

@Data
public class MonthlyTransactionCountDto {
    private YearMonth month;
    private Long transactionCount;  // оставляем прежнее название для закрытых транзакций
     private long onTimeCount;     // Кол-во транзакций, завершённых в срок (percentagePlanPpp >= 100%)
    private long delayedCount;   // percentagePlanPpp <= 100%
}