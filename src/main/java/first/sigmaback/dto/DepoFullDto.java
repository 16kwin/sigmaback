package first.sigmaback.dto;

import java.util.List;
import lombok.Data;

@Data
public class DepoFullDto {
    private DepoHeaderDto header;  // Добавляем AnalisHeaderDTO
    private List<DepoDto> transactions;
}