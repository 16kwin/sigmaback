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
    private final TimeService timeService; // Добавляем TimeService

    @Autowired
    public AnalisService(PppRepository pppRepository, AnalisHeaderService analisHeaderService, OperationService operationService, TimeService timeService) {
        this.pppRepository = pppRepository;
        this.analisHeaderService = analisHeaderService;
        this.operationService = operationService;
        this.timeService = timeService;
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

        // Получаем нормы и время работы по профессиям для данной транзакции из OperationService
        Map<String, String> results = operationService.calculateNormsByProfession(ppp.getTransaction());

        // Получаем нормы для каждой профессии
        String mechanicNorm = results.get("Механик");
        String electronNorm = results.get("Электронщик");
        String electricNorm = results.get("Электрик");
        String techNorm = results.get("Технолог");

        // Получаем время работы для каждой профессии (с префиксом "option")
        String mechanicOptionWorktype = results.get("mechanicOption");
        String electronOptionWorktype = results.get("electronOption");
        String electricOptionWorktype = results.get("electricOption");
        String techOptionWorktype = results.get("techOption");

        // Устанавливаем нормы и время работы для каждой профессии в DTO
        dto.setMechanicNorm(mechanicNorm != null ? Double.parseDouble(mechanicNorm) : 0.0);
        dto.setElectronNorm(electronNorm != null ? Double.parseDouble(electronNorm) : 0.0);
        dto.setElectricNorm(electricNorm != null ? Double.parseDouble(electricNorm) : 0.0);
        dto.setTechNorm(techNorm != null ? Double.parseDouble(techNorm) : 0.0);
        dto.setMechanicOptionWorktype(mechanicOptionWorktype);
        dto.setElectronOptionWorktype(electronOptionWorktype);
        dto.setElectricOptionWorktype(electricOptionWorktype);
        dto.setTechOptionWorktype(techOptionWorktype);

        // Получаем данные из TimeService
        Map<String, Map<String, String>> timeServiceResults = timeService.calculateOperationTimes(ppp.getTransaction());

        // Устанавливаем значения для каждой операции
        dto.setVhodControlStartTime(getOperationValue(timeServiceResults, "Входной контроль", "startTime"));
        dto.setVhodControlStopTime(getOperationValue(timeServiceResults, "Входной контроль", "stopTime"));
        dto.setVhodControlWorkTime(getOperationValue(timeServiceResults, "Входной контроль", "workTime"));

        dto.setPodkluchenieStartTime(getOperationValue(timeServiceResults, "Подключение", "startTime"));
        dto.setPodkluchenieStopTime(getOperationValue(timeServiceResults, "Подключение", "stopTime"));
        dto.setPodkluchenieWorkTime(getOperationValue(timeServiceResults, "Подключение", "workTime"));

        dto.setProverkaMehanikomStartTime(getOperationValue(timeServiceResults, "Проверка механиком", "startTime"));
        dto.setProverkaMehanikomStopTime(getOperationValue(timeServiceResults, "Проверка механиком", "stopTime"));
        dto.setProverkaMehanikomWorkTime(getOperationValue(timeServiceResults, "Проверка механиком", "workTime"));

        dto.setProverkaElectronStartTime(getOperationValue(timeServiceResults, "Проверка электронщиком", "startTime"));
        dto.setProverkaElectronStopTime(getOperationValue(timeServiceResults, "Проверка электронщиком", "stopTime"));
        dto.setProverkaElectronWorkTime(getOperationValue(timeServiceResults, "Проверка электронщиком", "workTime"));

        dto.setProverkaTehnologomStartTime(getOperationValue(timeServiceResults, "Проверка технологом", "startTime"));
        dto.setProverkaTehnologomStopTime(getOperationValue(timeServiceResults, "Проверка технологом", "stopTime"));
        dto.setProverkaTehnologomWorkTime(getOperationValue(timeServiceResults, "Проверка технологом", "workTime"));

        dto.setVihodControlStartTime(getOperationValue(timeServiceResults, "Выходной контроль", "startTime"));
        dto.setVihodControlStopTime(getOperationValue(timeServiceResults, "Выходной контроль", "stopTime"));
        dto.setVihodControlWorkTime(getOperationValue(timeServiceResults, "Выходной контроль", "workTime"));

        dto.setTransportPolozhenieStartTime(getOperationValue(timeServiceResults, "Транспортное положение", "startTime"));
        dto.setTransportPolozhenieStopTime(getOperationValue(timeServiceResults, "Транспортное положение", "stopTime"));
        dto.setTransportPolozhenieWorkTime(getOperationValue(timeServiceResults, "Транспортное положение", "workTime"));

        // Суммируем время и устанавливаем в DTO
        dto.setMechanicTotalWorktime(sumWorkTimes(mechanicOptionWorktype, getOperationValue(timeServiceResults, "Проверка механиком", "workTime")));
        dto.setElectronTotalWorktime(sumWorkTimes(electronOptionWorktype, getOperationValue(timeServiceResults, "Проверка электронщиком", "workTime")));
        dto.setElectricTotalWorktime(sumWorkTimes(electricOptionWorktype, getOperationValue(timeServiceResults, "Подключение", "workTime")));
        dto.setTechTotalWorktime(sumWorkTimes(techOptionWorktype, getOperationValue(timeServiceResults, "проверка технологом", "workTime")));

        return dto;
    }

    // Helper method to get operation value from TimeService results
    private String getOperationValue(Map<String, Map<String, String>> timeServiceResults, String operationName, String valueName) {
        if (timeServiceResults.containsKey(operationName) && timeServiceResults.get(operationName).containsKey(valueName)) {
            return timeServiceResults.get(operationName).get(valueName);
        }
        return "00:00:00"; // Or return a default value like "00:00:00" if you prefer
    }

    // Helper method to sum two work times in "HH:mm:ss" format
   private String sumWorkTimes(String time1, String time2) {
        long totalSeconds = 0;

        if (time1 != null && !time1.isEmpty()) {
            totalSeconds += parseTimeToSeconds(time1);
        }

        if (time2 != null && !time2.isEmpty()) {
            totalSeconds += parseTimeToSeconds(time2);
        }

        long HH = totalSeconds / 3600;
        long MM = (totalSeconds % 3600) / 60;
        long SS = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", HH, MM, SS);
    }

    // Helper method to parse time string in "HH:mm:ss" format to seconds
    private long parseTimeToSeconds(String time) {
        String[] parts = time.split(":");
        long HH = Long.parseLong(parts[0]);
        long MM = Long.parseLong(parts[1]);
        long SS = Long.parseLong(parts[2]);

        return HH * 3600 + MM * 60 + SS;
    }
}