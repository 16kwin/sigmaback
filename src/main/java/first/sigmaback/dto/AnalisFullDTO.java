package first.sigmaback.dto;

import java.util.List;
import lombok.Data;

@Data
public class AnalisFullDTO {
    private AnalisHeaderDTO header;  // Добавляем AnalisHeaderDTO
    private List<AnalisDTO> transactions;

}
