package first.sigmaback.service;

import first.sigmaback.dto.AnalisDTO;
import first.sigmaback.dto.AnalisFullDTO;
import first.sigmaback.dto.AnalisHeaderDTO;
import first.sigmaback.entity.Ppp;
import first.sigmaback.repository.PppRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalisService {

    private final PppRepository pppRepository;
    private final AnalisHeaderService analisHeaderService;
    private final OperationService operationService; // Добавляем OperationService

    @Autowired
    public AnalisService(PppRepository pppRepository, AnalisHeaderService analisHeaderService, OperationService operationService) {
        this.pppRepository = pppRepository;
        this.analisHeaderService = analisHeaderService;
        this.operationService = operationService; // Инициализируем OperationService
    }

    public AnalisFullDTO getAllTransactions() {
        // 1. Получаем данные для заголовка
        AnalisHeaderDTO header = analisHeaderService.getNorms();

        // 2. Получаем данные о транзакциях
        List<Ppp> ppps = pppRepository.findAll();

        // 3. Преобразуем Ppp в AnalisDTO
        List<AnalisDTO> analisDTOs = ppps.stream()
                .map(this::convertToAnalisDTO)
                .collect(Collectors.toList());

        // 4. Создаем AnalisFullDTO и заполняем данными
        AnalisFullDTO fullDTO = new AnalisFullDTO();
        fullDTO.setHeader(header);
        fullDTO.setTransactions(analisDTOs);

        return fullDTO;
    }

    private AnalisDTO convertToAnalisDTO(Ppp ppp) {
        AnalisDTO dto = new AnalisDTO();
        dto.setTransaction(ppp.getTransaction());
        dto.setStatus(ppp.getStatus());
        dto.setPlanPpp(ppp.getPlanPpp() * 8);
        dto.setPlanDateStart(ppp.getPlanDateStart());
        dto.setForecastDateStart(ppp.getForecastDateStart());
        dto.setFactDateStart(ppp.getFactDateStart());
        dto.setPlanDateStop(ppp.getPlanDateStop());
        dto.setForecastDateStop(ppp.getForecastDateStop());
        dto.setFactDateStop(ppp.getFactDateStop());
        dto.setPlanDateShipment(ppp.getPlanDateShipment());
        dto.setForecastDateShipment(ppp.getForecastDateShipment());
        dto.setFactDateShipment(ppp.getFactDateShipment());

        // Получаем нормы по профессиям для данной транзакции из OperationService
        Map<String, Double> normsByProfession = operationService.calculateNormsByProfession(ppp.getTransaction());

        // Получаем количество операций для каждой профессии
        Double mechanicOptionNorm = normsByProfession.get("Механик");
        Double electronOptionNorm = normsByProfession.get("Электронщик");
        Double electricOptionNorm = normsByProfession.get("Электрик");
        Double techOptionNorm = normsByProfession.get("Технолог");

        // Устанавливаем количество операций для каждой профессии в DTO
        dto.setMechanicOptionNorm(mechanicOptionNorm != null ? mechanicOptionNorm : 0.0);
        dto.setElectronOptionNorm(electronOptionNorm != null ? electronOptionNorm : 0.0);
        dto.setElectricOptionNorm(electricOptionNorm != null ? electricOptionNorm : 0.0);
        dto.setTechOptionNorm(techOptionNorm != null ? techOptionNorm : 0.0);

        return dto;
    }
}