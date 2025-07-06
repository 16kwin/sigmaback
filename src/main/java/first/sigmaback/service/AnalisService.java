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
    private final ProblemService problemService;


    @Autowired
    public AnalisService(PppRepository pppRepository, AnalisHeaderService analisHeaderService, OperationService operationService, TimeService timeService, ProblemService problemService) {
    this.pppRepository = pppRepository;
    this.analisHeaderService = analisHeaderService;
    this.operationService = operationService;
    this.timeService = timeService;
    this.problemService = problemService;
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

        dto.setMechanicTotalWorktime(sumWorkTimes(mechanicOptionWorktype, getOperationValue(timeServiceResults, "Проверка механиком", "workTime")));
        dto.setElectronTotalWorktime(sumWorkTimes(electronOptionWorktype, getOperationValue(timeServiceResults, "Проверка электронщиком", "workTime")));
        dto.setElectricTotalWorktime(sumWorkTimes(electricOptionWorktype, getOperationValue(timeServiceResults, "Подключение", "workTime")));
        dto.setTechTotalWorktime(sumWorkTimes(techOptionWorktype, getOperationValue(timeServiceResults, "Проверка технологом", "workTime")));

        // Calculate totalOperationsWorkTime
        String totalOperationsWorkTime = sumWorkTimes(
                dto.getVhodControlWorkTime(),
                sumWorkTimes(dto.getMechanicTotalWorktime(),
                        sumWorkTimes(dto.getElectronTotalWorktime(),
                                sumWorkTimes(dto.getElectricTotalWorktime(),
                                        sumWorkTimes(dto.getTechTotalWorktime(),
                                                sumWorkTimes(dto.getVihodControlWorkTime(),
                                                        dto.getTransportPolozhenieWorkTime()))))));

        dto.setTotalOperationsWorkTime(totalOperationsWorkTime);

            Map<String, Double> problemHoursByProfession = problemService.getProblemHoursByProfession(ppp.getTransaction());

    // Устанавливаем значения в DTO, если они есть, иначе 0.0
    dto.setMechanicProblemHours(problemHoursByProfession.getOrDefault("Механик", 0.0));
    dto.setElectronProblemHours(problemHoursByProfession.getOrDefault("Электронщик", 0.0));
    dto.setElectricProblemHours(problemHoursByProfession.getOrDefault("Электрик", 0.0));
    dto.setTechProblemHours(problemHoursByProfession.getOrDefault("Технолог", 0.0));
    dto.setComplexProblemHours(problemHoursByProfession.getOrDefault("Комплектация", 0.0));
 Double totalProblemHours = dto.getMechanicProblemHours() +
            dto.getElectronProblemHours() +
            dto.getElectricProblemHours() +
            dto.getTechProblemHours() +
            dto.getComplexProblemHours();

    dto.setTotalProblemHours(totalProblemHours);




    

String vhodNormString = analisHeaderService.getNorms().getVhodNorm(); // Получаем vhodNorm как String
double vhodNorm;

try {
    vhodNorm = Double.parseDouble(vhodNormString); // Преобразуем String в double
} catch (NumberFormatException e) {
    // Обработка ошибки, если строка не может быть преобразована в число
    System.err.println("Ошибка: Невозможно преобразовать vhodNorm в число. Установлено значение по умолчанию 0.0");
    vhodNorm = 0.0; // Устанавливаем значение по умолчанию
}


long vhodControlWorkTimeSeconds = parseTimeToSeconds(dto.getVhodControlWorkTime()); // Преобразуем в секунды
long vhodNormSeconds = (long) (vhodNorm * 3600); // Преобразуем vhodNorm в секунды

String vhodControlTimeExceeded = (vhodControlWorkTimeSeconds <= vhodNormSeconds) ? "Да" : "Нет";
dto.setVhodControlTimeExceeded(vhodControlTimeExceeded);



// Сравнение времени электрика (обновленная логика)

// Получаем значения
String podklyuchenieNormString = analisHeaderService.getNorms().getPodklyuchenieNorm(); // Получаем podklyuchenieNorm как String
String electricNormString = results.get("Электрик"); // Получаем electricNorm из OperationService
double podklyuchenieNorm;
double electricNormFull; // Объявляем electricNorm только один раз

// Преобразуем podklyuchenieNorm в double
try {
    podklyuchenieNorm = Double.parseDouble(podklyuchenieNormString);
} catch (NumberFormatException e) {
    System.err.println("Ошибка: Невозможно преобразовать podklyuchenieNorm в число. Установлено значение по умолчанию 0.0");
    podklyuchenieNorm = 0.0;
}

// Преобразуем electricNorm в double
try {
    electricNormFull = Double.parseDouble(electricNormString);
} catch (NumberFormatException e) {
    System.err.println("Ошибка: Невозможно преобразовать electricNormFull в число. Установлено значение по умолчанию 0.0");
    electricNormFull = 0.0;
}

// Преобразуем electricTotalWorktime и electricProblemHours в секунды
long electricTotalWorkTimeSeconds = parseTimeToSeconds(dto.getElectricTotalWorktime());
double electricProblemHours = dto.getElectricProblemHours();
long electricProblemHoursSeconds = (long) (electricProblemHours * 3600);

// Вычитаем время проблем из общего времени
long cleanElectricTimeSeconds = electricTotalWorkTimeSeconds - electricProblemHoursSeconds;

// Суммируем нормативы
double totalElectricNorm = podklyuchenieNorm + electricNormFull;
long totalElectricNormSeconds = (long) (totalElectricNorm * 3600);

// Сравниваем и устанавливаем результат
String electricTimeExceeded = (cleanElectricTimeSeconds < totalElectricNormSeconds) ? "Да" : "Нет";
dto.setElectricTimeExceeded(electricTimeExceeded);



String mechOperationNormString = analisHeaderService.getNorms().getMechOperationNorm(); // Получаем mechOperationNorm как String
String mechNormString = results.get("Электрик"); // Получаем mechNorm из OperationService
double mechOperationNorm;
double mechNormFull; // Объявляем mechNorm только один раз

// Преобразуем mechOperationNorm в double
try {
    mechOperationNorm = Double.parseDouble(mechOperationNormString);
} catch (NumberFormatException e) {
    System.err.println("Ошибка: Невозможно преобразовать mechOperationNorm в число. Установлено значение по умолчанию 0.0");
    mechOperationNorm = 0.0;
}

// Преобразуем mechNorm в double
try {
    mechNormFull = Double.parseDouble(mechNormString);
} catch (NumberFormatException e) {
    System.err.println("Ошибка: Невозможно преобразовать mechNormFull в число. Установлено значение по умолчанию 0.0");
    mechNormFull = 0.0;
}

// Преобразуем mechTotalWorktime и mechProblemHours в секунды
long mechTotalWorkTimeSeconds = parseTimeToSeconds(dto.getMechanicTotalWorktime());
double mechProblemHours = dto.getMechanicProblemHours();
long mechProblemHoursSeconds = (long) (mechProblemHours * 3600);

// Вычитаем время проблем из общего времени
long cleanmechTimeSeconds = mechTotalWorkTimeSeconds - mechProblemHoursSeconds;

// Суммируем нормативы
double totalmechNorm = mechOperationNorm + mechNormFull;
long totalmechNormSeconds = (long) (totalmechNorm * 3600);

// Сравниваем и устанавливаем результат
String mechTimeExceeded = (cleanmechTimeSeconds < totalmechNormSeconds) ? "Да" : "Нет";
dto.setMechanicTimeExceeded(mechTimeExceeded);






String electronOperationNormString = analisHeaderService.getNorms().getElectronOperationNorm(); // Получаем electronOperationNorm как String
String electronNormString = results.get("Электрик"); // Получаем electronNorm из OperationService
double electronOperationNorm;
double electronNormFull; // Объявляем electronNorm только один раз

// Преобразуем electronOperationNorm в double
try {
    electronOperationNorm = Double.parseDouble(electronOperationNormString);
} catch (NumberFormatException e) {
    System.err.println("Ошибка: Невозможно преобразовать electronOperationNorm в число. Установлено значение по умолчанию 0.0");
    electronOperationNorm = 0.0;
}

// Преобразуем electronNorm в double
try {
    electronNormFull = Double.parseDouble(electronNormString);
} catch (NumberFormatException e) {
    System.err.println("Ошибка: Невозможно преобразовать electronNormFull в число. Установлено значение по умолчанию 0.0");
    electronNormFull = 0.0;
}

// Преобразуем electronTotalWorktime и electronProblemHours в секунды
long electronTotalWorkTimeSeconds = parseTimeToSeconds(dto.getElectronTotalWorktime());
double electronProblemHours = dto.getElectronProblemHours();
long electronProblemHoursSeconds = (long) (electronProblemHours * 3600);

// Вычитаем время проблем из общего времени
long cleanElectronTimeSeconds = electronTotalWorkTimeSeconds - electronProblemHoursSeconds;

// Суммируем нормативы
double totalElectronNorm = podklyuchenieNorm + electronNormFull;
long totalElectronNormSeconds = (long) (totalElectronNorm * 3600);

// Сравниваем и устанавливаем результат
String electronTimeExceeded = (cleanElectronTimeSeconds < totalElectronNormSeconds) ? "Да" : "Нет";
dto.setElectronTimeExceeded(electronTimeExceeded);





String techOperationNormString = analisHeaderService.getNorms().getTechOperationNorm(); // Получаем techOperationNorm как String
String techNormString = results.get("Электрик"); // Получаем techNorm из OperationService
double techOperationNorm;
double techNormFull; // Объявляем techNorm только один раз

// Преобразуем techOperationNorm в double
try {
    techOperationNorm = Double.parseDouble(techOperationNormString);
} catch (NumberFormatException e) {
    System.err.println("Ошибка: Невозможно преобразовать podklyuchenieNorm в число. Установлено значение по умолчанию 0.0");
    podklyuchenieNorm = 0.0;
}

// Преобразуем techNorm в double
try {
    techNormFull = Double.parseDouble(techNormString);
} catch (NumberFormatException e) {
    System.err.println("Ошибка: Невозможно преобразовать techNormFull в число. Установлено значение по умолчанию 0.0");
    techNormFull = 0.0;
}

// Преобразуем techTotalWorktime и techProblemHours в секунды
long techTotalWorkTimeSeconds = parseTimeToSeconds(dto.getTechTotalWorktime());
double techProblemHours = dto.getTechProblemHours();
long techProblemHoursSeconds = (long) (techProblemHours * 3600);

// Вычитаем время проблем из общего времени
long cleantechTimeSeconds = techTotalWorkTimeSeconds - techProblemHoursSeconds;

// Суммируем нормативы
double totaltechNorm = podklyuchenieNorm + techNormFull;
long totaltechNormSeconds = (long) (totaltechNorm * 3600);

// Сравниваем и устанавливаем результат
String techTimeExceeded = (cleantechTimeSeconds < totaltechNormSeconds) ? "Да" : "Нет";
dto.setTechTimeExceeded(techTimeExceeded);




String vihodNormString = analisHeaderService.getNorms().getVihodNorm(); // Получаем vihodNorm как String
double vihodNorm;

try {
    vihodNorm = Double.parseDouble(vihodNormString); // Преобразуем String в double
} catch (NumberFormatException e) {
    // Обработка ошибки, если строка не может быть преобразована в число
    System.err.println("Ошибка: Невозможно преобразовать vihodNorm в число. Установлено значение по умолчанию 0.0");
    vihodNorm = 0.0; // Устанавливаем значение по умолчанию
}


long vihodControlWorkTimeSeconds = parseTimeToSeconds(dto.getVihodControlWorkTime()); // Преобразуем в секунды
long vihodNormSeconds = (long) (vihodNorm * 3600); // Преобразуем vihodNorm в секунды

String vihodControlTimeExceeded = (vihodControlWorkTimeSeconds <= vihodNormSeconds) ? "Да" : "Нет";
dto.setVihodControlTimeExceeded(vihodControlTimeExceeded);


String transportNormString = analisHeaderService.getNorms().getTransportNorm(); // Получаем transportNorm как String
double transportNorm;

try {
    transportNorm = Double.parseDouble(transportNormString); // Преобразуем String в double
} catch (NumberFormatException e) {
    // Обработка ошибки, если строка не может быть преобразована в число
    System.err.println("Ошибка: Невозможно преобразовать transportNorm в число. Установлено значение по умолчанию 0.0");
    transportNorm = 0.0; // Устанавливаем значение по умолчанию
}


long transportWorkTimeSeconds = parseTimeToSeconds(dto.getTransportPolozhenieWorkTime()); // Преобразуем в секунды
long transportNormSeconds = (long) (transportNorm * 3600); // Преобразуем transportNorm в секунды

String transportTimeExceeded = (transportWorkTimeSeconds <= transportNormSeconds) ? "Да" : "Нет";
dto.setTransportTimeExceeded(transportTimeExceeded);


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