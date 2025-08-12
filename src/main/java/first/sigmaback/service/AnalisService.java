package first.sigmaback.service;

import first.sigmaback.dto.AnalisDTO;
import first.sigmaback.dto.AnalisFullDTO;
import first.sigmaback.dto.AnalisHeaderDTO;
import first.sigmaback.entity.Ppp;
import first.sigmaback.repository.PppRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class AnalisService {

    private final PppRepository pppRepository;
    private final AnalisHeaderService analisHeaderService;
    private final OperationService operationService; // Добавляем OperationService
    private final TimeService timeService; // Добавляем TimeService
    private final ProblemService problemService;
    private final DatesService datesService;
private final EmployeesService employeesService;
private final InterService interService; // Добавляем зависимость

@Autowired
public AnalisService(PppRepository pppRepository, AnalisHeaderService analisHeaderService, OperationService operationService, TimeService timeService, ProblemService problemService, DatesService datesService, EmployeesService employeesService, InterService interService) {
    this.pppRepository = pppRepository;
    this.analisHeaderService = analisHeaderService;
    this.operationService = operationService;
    this.timeService = timeService;
    this.problemService = problemService;
    this.datesService = datesService;
    this.employeesService = employeesService;
    this.interService = interService; // Инициализируем зависимость
}


    public AnalisFullDTO getAllTransactions() {
        // 1. Получаем данные для заголовка
        AnalisHeaderDTO header = analisHeaderService.getNorms();
        // 2. Получаем и преобразуем транзакции
        List<Ppp> ppps = pppRepository.findAll();
        List<AnalisDTO> transactions = ppps.stream()
                .map(this::convertToAnalisDTO)
                .collect(Collectors.toList());

    long overfulfilledCount = 0;
    long underfulfilledCount = 0;

    for (AnalisDTO dto : transactions) {
        if (dto.getPercentagePlanPpp() != null) {
            double percentage = parsePercentage(dto.getPercentagePlanPpp());
            if (percentage >= 100.0) {
                overfulfilledCount++;
            } else {
                underfulfilledCount++;
            }
        }
    }

    // 3. Обновляем заголовок
    header.setOverfulfilledTransactionsCount(overfulfilledCount);
    header.setUnderfulfilledTransactionsCount(underfulfilledCount);

        // 4. Возвращаем результат
        AnalisFullDTO fullDTO = new AnalisFullDTO();
        fullDTO.setHeader(header);
        fullDTO.setTransactions(transactions);
        return fullDTO;
    }




     private AnalisDTO convertToAnalisDTO(Ppp ppp) {
        AnalisDTO dto = new AnalisDTO();
        dto.setTransaction(ppp.getTransaction());
        dto.setStatus(ppp.getStatus());
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
        dto.setTotalProfessionNorms(
        (dto.getMechanicNorm() != null ? dto.getMechanicNorm() : 0.0) +
        (dto.getElectronNorm() != null ? dto.getElectronNorm() : 0.0) +
        (dto.getElectricNorm() != null ? dto.getElectricNorm() : 0.0) +
        (dto.getTechNorm() != null ? dto.getTechNorm() : 0.0)
    );
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

String mechanicWorkTime = getOperationValue(timeServiceResults, "Проверка механиком", "workTime");
long mechanicOptionSeconds = parseTimeToSeconds(mechanicOptionWorktype);
long mechanicWorkSeconds = parseTimeToSeconds(mechanicWorkTime);

if (mechanicOptionSeconds == -1 || mechanicWorkSeconds == -1) {
    dto.setMechanicTotalWorktime("Нет данных");
} else {
    dto.setMechanicTotalWorktime(sumWorkTimes(mechanicOptionWorktype, mechanicWorkTime));
}

String electronWorkTime = getOperationValue(timeServiceResults, "Проверка электронщиком", "workTime");
long electronOptionSeconds = parseTimeToSeconds(electronOptionWorktype);
long electronWorkSeconds = parseTimeToSeconds(electronWorkTime);

if (electronOptionSeconds == -1 || electronWorkSeconds == -1) {
    dto.setElectronTotalWorktime("Нет данных");
} else {
    dto.setElectronTotalWorktime(sumWorkTimes(electronOptionWorktype, electronWorkTime));
}

String electricWorkTime = getOperationValue(timeServiceResults, "Подключение", "workTime");
long electricOptionSeconds = parseTimeToSeconds(electricOptionWorktype);
long electricWorkSeconds = parseTimeToSeconds(electricWorkTime);

if (electricOptionSeconds == -1 || electricWorkSeconds == -1) {
    dto.setElectricTotalWorktime("Нет данных");
} else {
    dto.setElectricTotalWorktime(sumWorkTimes(electricOptionWorktype, electricWorkTime));
}

String techWorkTime = getOperationValue(timeServiceResults, "Проверка технологом", "workTime");
long techOptionSeconds = parseTimeToSeconds(techOptionWorktype);
long techWorkSeconds = parseTimeToSeconds(techWorkTime);

if (techOptionSeconds == -1 || techWorkSeconds == -1) {
    dto.setTechTotalWorktime("Нет данных");
} else {
    dto.setTechTotalWorktime(sumWorkTimes(techOptionWorktype, techWorkTime));
}



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

String vhodControlTimeExceeded;
if (vhodControlWorkTimeSeconds == -1) {
    vhodControlTimeExceeded = "Нет данных";
} else if (vhodControlWorkTimeSeconds >= 0 && vhodControlWorkTimeSeconds <= 300) {  // Добавлено
    vhodControlTimeExceeded = "Контроль руководителя";  // Добавлено
} else if (vhodControlWorkTimeSeconds == 0) {
    vhodControlTimeExceeded = "100.00%";
} else {
    double percentageExceeded = (  vhodControlWorkTimeSeconds/(double) vhodNormSeconds) * 100;
    vhodControlTimeExceeded = String.format("%.2f%%", percentageExceeded);
}
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
long cleanElectricTimeSeconds = (electricTotalWorkTimeSeconds == -1 || electricProblemHoursSeconds == -1) ? -1 : electricTotalWorkTimeSeconds - electricProblemHoursSeconds;

// Суммируем нормативы
double totalElectricNorm = podklyuchenieNorm + electricNormFull;
long totalElectricNormSeconds = (long) (totalElectricNorm * 3600);

String electricTimeExceeded;
if (cleanElectricTimeSeconds == -1) {
    electricTimeExceeded = "Нет данных";
} else if (cleanElectricTimeSeconds >= 0 && cleanElectricTimeSeconds <= 300) {  // Добавлено
    electricTimeExceeded = "Контроль руководителя";  // Добавлено
} else if (cleanElectricTimeSeconds == 0) {
    electricTimeExceeded = "100.00%";
} else {
    double percentageExceeded = (  cleanElectricTimeSeconds/(double) totalElectricNormSeconds) * 100;
    electricTimeExceeded = String.format("%.2f%%", percentageExceeded);
}
dto.setElectricTimeExceeded(electricTimeExceeded);



String mechOperationNormString = analisHeaderService.getNorms().getMechOperationNorm(); // Получаем mechOperationNorm как String
String mechNormString = results.get("Механик"); // Получаем mechNorm из OperationService
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
long cleanmechTimeSeconds = (mechTotalWorkTimeSeconds == -1 || mechProblemHoursSeconds == -1) ? -1 : mechTotalWorkTimeSeconds - mechProblemHoursSeconds;

// Суммируем нормативы
double totalmechNorm = mechOperationNorm + mechNormFull;
long totalmechNormSeconds = (long) (totalmechNorm * 3600);

String mechTimeExceeded;
if (cleanmechTimeSeconds == -1) {
    mechTimeExceeded = "Нет данных";
} else if (cleanmechTimeSeconds >= 0 && cleanmechTimeSeconds <= 300) {  // Добавлено
    mechTimeExceeded = "Контроль руководителя";  // Добавлено
} else if (cleanmechTimeSeconds == 0) {
    mechTimeExceeded = "100.00%";
} else {
    double percentageExceeded = ( cleanmechTimeSeconds/(double) totalmechNormSeconds) * 100;
    mechTimeExceeded = String.format("%.2f%%", percentageExceeded);
}
dto.setMechanicTimeExceeded(mechTimeExceeded);






String electronOperationNormString = analisHeaderService.getNorms().getElectronOperationNorm(); // Получаем electronOperationNorm как String
String electronNormString = results.get("Электронщик"); // Получаем electronNorm из OperationService
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
long cleanElectronTimeSeconds = (electronTotalWorkTimeSeconds == -1 || electronProblemHoursSeconds == -1) ? -1 : electronTotalWorkTimeSeconds - electronProblemHoursSeconds ;

// Суммируем нормативы
double totalElectronNorm = electronOperationNorm + electronNormFull;
long totalElectronNormSeconds = (long) (totalElectronNorm * 3600);
String electronTimeExceeded;
if (cleanElectronTimeSeconds == -1) {
    electronTimeExceeded = "Нет данных";
} else if (cleanElectronTimeSeconds >= 0 && cleanElectronTimeSeconds <= 300) {  // Добавлено
    electronTimeExceeded = "Контроль руководителя";  // Добавлено
} else if (cleanElectronTimeSeconds == 0) {
    electronTimeExceeded = "100.00%";
} else {
    double percentageExceeded = ( cleanElectronTimeSeconds/(double) totalElectronNormSeconds) * 100;
    electronTimeExceeded = String.format("%.2f%%", percentageExceeded);
}
dto.setElectronTimeExceeded(electronTimeExceeded);





String techOperationNormString = analisHeaderService.getNorms().getTechOperationNorm(); // Получаем techOperationNorm как String
String techNormString = results.get("Технолог"); // Получаем techNorm из OperationService
double techOperationNorm;
double techNormFull; // Объявляем techNorm только один раз

// Преобразуем techOperationNorm в double
try {
    techOperationNorm = Double.parseDouble(techOperationNormString);
} catch (NumberFormatException e) {
    System.err.println("Ошибка: Невозможно преобразовать podklyuchenieNorm в число. Установлено значение по умолчанию 0.0");
    techOperationNorm = 0.0;
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
long cleantechTimeSeconds = (techTotalWorkTimeSeconds == -1 || techProblemHoursSeconds == -1) ? -1 : techTotalWorkTimeSeconds - techProblemHoursSeconds;

// Суммируем нормативы
double totaltechNorm = techOperationNorm + techNormFull;
long totaltechNormSeconds = (long) (totaltechNorm * 3600);

String techTimeExceeded;
if (cleantechTimeSeconds == -1) {
    techTimeExceeded = "Нет данных";
} else if (cleantechTimeSeconds >= 0 && cleantechTimeSeconds <= 300) {  // Добавлено
    techTimeExceeded = "Контроль руководителя";  // Добавлено
} else if (cleantechTimeSeconds == 0) {
    techTimeExceeded = "100.00%";
} else {
    double percentageExceeded = ( cleantechTimeSeconds/(double) totaltechNormSeconds) * 100;
    techTimeExceeded = String.format("%.2f%%", percentageExceeded);
}
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

String vihodControlTimeExceeded;
if (vihodControlWorkTimeSeconds == -1 || vihodControlWorkTimeSeconds == 0) {
    vihodControlTimeExceeded = "Нет данных";
} else if (vihodControlWorkTimeSeconds >= 0 && vihodControlWorkTimeSeconds <= 300) {  // Добавлено
    vihodControlTimeExceeded = "Контроль руководителя";  // Добавлено
} else {
    double percentageExceeded = ( vihodControlWorkTimeSeconds/(double) vihodNormSeconds) * 100;
    vihodControlTimeExceeded = String.format("%.2f%%", percentageExceeded);
}
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
String transportTimeExceeded;
if (transportWorkTimeSeconds == -1 || transportNormSeconds == 0) {
    transportTimeExceeded = "Нет данных";
} else if (transportWorkTimeSeconds >= 0 && transportWorkTimeSeconds <= 300) {  // Добавлено
    transportTimeExceeded = "Контроль руководителя";  // Добавлено
} else {
    double percentageExceeded = (  transportWorkTimeSeconds/(double) transportNormSeconds) * 100;
    transportTimeExceeded = String.format("%.2f%%", percentageExceeded);
}
dto.setTransportTimeExceeded(transportTimeExceeded);


 LocalDate planDateStart = ppp.getPlanDateStart();

    // Создаем карту с нормативами
    Map<Integer, Double> norms = new HashMap<>();

    norms.put(1, parseDouble(analisHeaderService.getNorms().getVhodNorm()));
    norms.put(2, parseDouble(analisHeaderService.getNorms().getPodklyuchenieNorm()) + parseDouble(results.get("Электрик")));
    norms.put(3, parseDouble(analisHeaderService.getNorms().getMechOperationNorm()) + parseDouble(results.get("Механик")));
    norms.put(4, parseDouble(analisHeaderService.getNorms().getElectronOperationNorm()) + parseDouble(results.get("Электронщик")));
    norms.put(5, parseDouble(analisHeaderService.getNorms().getTechOperationNorm()) + parseDouble(results.get("Технолог")));
    norms.put(6, parseDouble(analisHeaderService.getNorms().getVihodNorm()));
    norms.put(7, parseDouble(analisHeaderService.getNorms().getTransportNorm()));

    // Вызываем DatesService
    Map<Integer, LocalDate> calculatedDates = datesService.calculateDates(planDateStart, norms);

    // Устанавливаем даты в DTO
    dto.setPlanDate1(calculatedDates.get(1));
    dto.setPlanDate2(calculatedDates.get(2));
    dto.setPlanDate3(calculatedDates.get(3));
    dto.setPlanDate4(calculatedDates.get(4));
    dto.setPlanDate5(calculatedDates.get(5));
    dto.setPlanDate6(calculatedDates.get(6));
    dto.setPlanDate7(calculatedDates.get(7));


LocalDate startDate;
if (ppp.getFactDateStart() != null) {
    startDate = ppp.getFactDateStart();
} else if (ppp.getForecastDateStart() != null) {
    startDate = ppp.getForecastDateStart();
} else {
    // Если ни factDateStart, ни forecastDateStart не заданы, используем planDateStart
    if (ppp.getPlanDateStart() != null) {
        startDate = ppp.getPlanDateStart(); // Или можно бросить исключение
    } else {
        // Если и planDateStart не задан, то у нас проблема!
        System.err.println("Критическая ошибка: factDateStart, forecastDateStart и planDateStart равны null.  Невозможно рассчитать даты.");
        // В этом случае лучше бросить исключение или вернуть null
        return null; // Или throw new IllegalStateException("...");
    }
}

    // Создаем карту с нормативами
    Map<Integer, Double> norms2 = new HashMap<>();

    norms2.put(1, parseDouble(analisHeaderService.getNorms().getVhodNorm()));
    norms2.put(2, parseDouble(analisHeaderService.getNorms().getPodklyuchenieNorm()) + parseDouble(results.get("Электрик")));
    norms2.put(3, parseDouble(analisHeaderService.getNorms().getMechOperationNorm()) + parseDouble(results.get("Механик")));
    norms2.put(4, parseDouble(analisHeaderService.getNorms().getElectronOperationNorm()) + parseDouble(results.get("Электронщик")));
    norms2.put(5, parseDouble(analisHeaderService.getNorms().getTechOperationNorm()) + parseDouble(results.get("Технолог")));
    norms2.put(6, parseDouble(analisHeaderService.getNorms().getVihodNorm()));
    norms2.put(7, parseDouble(analisHeaderService.getNorms().getTransportNorm()));

    // Вызываем DatesService, используя startDate
    Map<Integer, LocalDate> calculatedDates2 = datesService.calculateDates(startDate, norms2);

    // Устанавливаем даты в DTO
    dto.setFactDate1(calculatedDates2.get(1)); // Предполагаем, что у вас теперь есть поля FactDate1... FactDate7
    dto.setFactDate2(calculatedDates2.get(2));
    dto.setFactDate3(calculatedDates2.get(3));
    dto.setFactDate4(calculatedDates2.get(4));
    dto.setFactDate5(calculatedDates2.get(5));
    dto.setFactDate6(calculatedDates2.get(6));
    dto.setFactDate7(calculatedDates2.get(7));
String transactionId = ppp.getTransaction();
    Map<String, String> employeeNames = employeesService.getEmployeeNamesForTransaction(transactionId);

// Устанавливаем фамилии в DTO
dto.setVhodControlEmployee(employeeNames.get("Входной контроль"));
dto.setPodkluchenieEmployee(employeeNames.get("Подключение"));
dto.setProverkaMehanikomEmployee(employeeNames.get("Проверка механиком"));
dto.setProverkaElectronEmployee(employeeNames.get("Проверка электронщиком"));
dto.setProverkaTehnologomEmployee(employeeNames.get("Проверка технологом"));
dto.setVihodControlEmployee(employeeNames.get("Выходной контроль"));
dto.setTransportPolozhenieEmployee(employeeNames.get("Транспортное положение"));




Map<String, Map<String, String>> timeServiceResults2 = timeService.calculateOperationTimes(ppp.getTransaction());


// Получаем startTime и stopTime для операций, используя `timeServiceResults2`
String vhodControlStopTime = "Нет данных";
if (timeServiceResults2.containsKey("Входной контроль") && timeServiceResults2.get("Входной контроль").containsKey("stopTime")) {
    vhodControlStopTime = timeServiceResults2.get("Входной контроль").get("stopTime");
}

String podkluchenieStartTime = "Нет данных";
if (timeServiceResults2.containsKey("Подключение") && timeServiceResults2.get("Подключение").containsKey("startTime")) {
    podkluchenieStartTime = timeServiceResults2.get("Подключение").get("startTime");
}

String podkluchenieStopTime = "Нет данных";
if (timeServiceResults2.containsKey("Подключение") && timeServiceResults2.get("Подключение").containsKey("stopTime")) {
    podkluchenieStopTime = timeServiceResults2.get("Подключение").get("stopTime");
}

String proverkaMehanikomStartTime = "Нет данных";
if (timeServiceResults2.containsKey("Проверка механиком") && timeServiceResults2.get("Проверка механиком").containsKey("startTime")) {
    proverkaMehanikomStartTime = timeServiceResults2.get("Проверка механиком").get("startTime");
}
//  ... повторяем для остальных операций ...
String proverkaMehanikomStopTime = "Нет данных";
if (timeServiceResults2.containsKey("Проверка механиком") && timeServiceResults2.get("Проверка механиком").containsKey("stopTime")) {
    proverkaMehanikomStopTime = timeServiceResults2.get("Проверка механиком").get("stopTime");
}
String proverkaElectronStartTime = "Нет данных";
if (timeServiceResults2.containsKey("Проверка электронщиком") && timeServiceResults2.get("Проверка электронщиком").containsKey("startTime")) {
    proverkaElectronStartTime = timeServiceResults2.get("Проверка электронщиком").get("startTime");
}
String proverkaElectronStopTime = "Нет данных";
if (timeServiceResults2.containsKey("Проверка электронщиком") && timeServiceResults2.get("Проверка электронщиком").containsKey("stopTime")) {
    proverkaElectronStopTime = timeServiceResults2.get("Проверка электронщиком").get("stopTime");
}
String proverkaTehnologomStartTime = "Нет данных";
if (timeServiceResults2.containsKey("Проверка технологом") && timeServiceResults2.get("Проверка технологом").containsKey("startTime")) {
    proverkaTehnologomStartTime = timeServiceResults2.get("Проверка технологом").get("startTime");
}
String proverkaTehnologomStopTime = "Нет данных";
if (timeServiceResults2.containsKey("Проверка технологом") && timeServiceResults2.get("Проверка технологом").containsKey("stopTime")) {
    proverkaTehnologomStopTime = timeServiceResults2.get("Проверка технологом").get("stopTime");
}

String vihodControlStartTime = "Нет данных";
if (timeServiceResults2.containsKey("Выходной контроль") && timeServiceResults2.get("Выходной контроль").containsKey("startTime")) {
    vihodControlStartTime = timeServiceResults2.get("Выходной контроль").get("startTime");
}
String vihodControlStopTime = "Нет данных";
if (timeServiceResults2.containsKey("Выходной контроль") && timeServiceResults2.get("Выходной контроль").containsKey("stopTime")) {
    vihodControlStopTime = timeServiceResults2.get("Выходной контроль").get("stopTime");
}

String transportPolozhenieStartTime = "Нет данных";
if (timeServiceResults2.containsKey("Транспортное положение") && timeServiceResults2.get("Транспортное положение").containsKey("startTime")) {
    transportPolozhenieStartTime = timeServiceResults2.get("Транспортное положение").get("startTime");
}


// Вычисляем время между операциями, вызывая InterService
String timeBetweenVhodAndPodkluchenie = interService.calculateTimeBetweenOperations(vhodControlStopTime, podkluchenieStartTime);
String timeBetweenPodkluchenieAndProverkaMehanikom = interService.calculateTimeBetweenOperations(podkluchenieStopTime, proverkaMehanikomStartTime);
String timeBetweenProverkaMehanikomAndProverkaElectron = interService.calculateTimeBetweenOperations(proverkaMehanikomStopTime, proverkaElectronStartTime);
String timeBetweenProverkaElectronAndProverkaTehnologom = interService.calculateTimeBetweenOperations(proverkaElectronStopTime, proverkaTehnologomStartTime);
String timeBetweenProverkaTehnologomAndVihodControl = interService.calculateTimeBetweenOperations(proverkaTehnologomStopTime, vihodControlStartTime);
String timeBetweenVihodControlAndTransportPolozhenie = interService.calculateTimeBetweenOperations(vihodControlStopTime, transportPolozhenieStartTime);


// Устанавливаем значения в DTO
dto.setTimeBetweenVhodAndPodkluchenie(timeBetweenVhodAndPodkluchenie);
dto.setTimeBetweenPodkluchenieAndProverkaMehanikom(timeBetweenPodkluchenieAndProverkaMehanikom);
dto.setTimeBetweenProverkaMehanikomAndProverkaElectron(timeBetweenProverkaMehanikomAndProverkaElectron);
dto.setTimeBetweenProverkaElectronAndProverkaTehnologom(timeBetweenProverkaElectronAndProverkaTehnologom);
dto.setTimeBetweenProverkaTehnologomAndVihodControl(timeBetweenProverkaTehnologomAndVihodControl);
dto.setTimeBetweenVihodControlAndTransportPolozhenie(timeBetweenVihodControlAndTransportPolozhenie);


String timeBetweenVhodAndPodkluchenie2 = dto.getTimeBetweenVhodAndPodkluchenie();
String timeBetweenPodkluchenieAndProverkaMehanikom2 = dto.getTimeBetweenPodkluchenieAndProverkaMehanikom();
String timeBetweenProverkaMehanikomAndProverkaElectron2 = dto.getTimeBetweenProverkaMehanikomAndProverkaElectron();
String timeBetweenProverkaElectronAndProverkaTehnologom2 = dto.getTimeBetweenProverkaElectronAndProverkaTehnologom();
String timeBetweenProverkaTehnologomAndVihodControl2 = dto.getTimeBetweenProverkaTehnologomAndVihodControl();
String timeBetweenVihodControlAndTransportPolozhenie2 = dto.getTimeBetweenVihodControlAndTransportPolozhenie();

// Суммируем все времена
String totalTime = sumWorkTimes2(
        timeBetweenVhodAndPodkluchenie2,
        timeBetweenPodkluchenieAndProverkaMehanikom2,
        timeBetweenProverkaMehanikomAndProverkaElectron2,
        timeBetweenProverkaElectronAndProverkaTehnologom2,
        timeBetweenProverkaTehnologomAndVihodControl2,
        timeBetweenVihodControlAndTransportPolozhenie2
);

// Устанавливаем общую сумму в DTO
dto.setTotalTimeBetweenOperations(totalTime);


// Получаем planPpp из DTO (предполагается, что planPpp - это double в минутах)
int planPppMinutes = dto.getPlanPpp();

// Преобразуем planPpp в секунды
int planPppSeconds = planPppMinutes * 3600;

// Вычисляем totalOperationsWorkTime в секундах
long totalOperationsWorkTimeSeconds = 0;
for (Map.Entry<String, Map<String, String>> entry : timeServiceResults.entrySet()) {
    String operationName = entry.getKey();
    Map<String, String> operationInfo = entry.getValue();
    String workTime = operationInfo.get("workTime");
    long workTimeInSeconds = parseTimeToSeconds(workTime); // Получаем время в секундах
    if (workTimeInSeconds != -1) { // Проверяем, не равно ли -1
        totalOperationsWorkTimeSeconds += workTimeInSeconds; // Прибавляем, только если не равно -1
    }
}

// Вычисляем процент
double percentage;
if (totalOperationsWorkTimeSeconds == 0) {
    percentage = 0; // Если делим на 0, то процент равен 0
} else {
    percentage = (double) (planPppSeconds *100) / totalOperationsWorkTimeSeconds;
}

// Форматируем результат
String formattedPercentage = String.format("%.2f%%", percentage);

// Устанавливаем результат в DTO
dto.setPercentagePlanPpp(formattedPercentage);


// ... (existing code) ...

// Получаем значения из DTO
String totalOperationsWorkTime2 = dto.getTotalOperationsWorkTime(); // Используем totalOperationsWorkTime
double problemHours = dto.getTotalProblemHours();
String timeBetweenOperations = dto.getTotalTimeBetweenOperations();

// Преобразуем все значения в секунды
long workTimeInSeconds = parseTimeToSeconds(totalOperationsWorkTime2);
if (workTimeInSeconds == -1) {
    workTimeInSeconds = 0;
}

long problemTimeInSeconds = (long) (problemHours * 3600); // Преобразуем часы в секунды

long betweenTimeInSeconds = parseTimeToSeconds(timeBetweenOperations);
if (betweenTimeInSeconds == -1) {
    betweenTimeInSeconds = 0;
}

// Суммируем все значения в секундах
long totalTimeInSeconds = workTimeInSeconds - problemTimeInSeconds + betweenTimeInSeconds;

// Форматируем общую сумму секунд в "HH:mm:ss"
String formattedTotalTime = formatSecondsToTime(totalTimeInSeconds);

// Устанавливаем результат в DTO
dto.setTotalTimeAll(formattedTotalTime);

// ... (existing code) ...

        int totalNormsAndProblems = (int)(dto.getTotalProfessionNorms() + analisHeaderService.getTotalHeaderNorms());
                dto.setPlanPpp(totalNormsAndProblems);
        return dto;
    }





    // Helper method to get operation value from TimeService results
    private String getOperationValue(Map<String, Map<String, String>> timeServiceResults, String operationName, String valueName) {
        if (timeServiceResults.containsKey(operationName) && timeServiceResults.get(operationName).containsKey(valueName)) {
            return timeServiceResults.get(operationName).get(valueName);
        }
        return "00:00:00";
    }






    // Helper method to sum two work times in "HH:mm:ss" format
private String sumWorkTimes(String time1, String time2) {
    long totalSeconds = 0;

    long seconds1 = parseTimeToSeconds(time1);
    if (seconds1 != -1) {
        totalSeconds += seconds1;
    }

    long seconds2 = parseTimeToSeconds(time2);
    if (seconds2 != -1) {
        totalSeconds += seconds2;
    }

    if (seconds1 == -1 && seconds2 == -1) {
        return "Нет данных";
    }

    long HH = totalSeconds / 3600;
    long MM = (totalSeconds % 3600) / 60;
    long SS = totalSeconds % 60;

    return String.format("%02d:%02d:%02d", HH, MM, SS);
}


// Helper method to sum work times in "HH:mm:ss" format
private String sumWorkTimes2(String... times) {
    long totalSeconds = 0;

    for (String time : times) {
        long seconds = parseTimeToSeconds(time);
        if (seconds != -1) {
            totalSeconds += seconds;
        }
    }

   boolean allNoData = true;
    for (String time : times) {
        if (time != null && !time.isEmpty() && !time.equals("Нет данных")) {
            allNoData = false;
            break;
        }
    }

    if (allNoData) {
        return "Нет данных";
    }

    long HH = totalSeconds / 3600;
    long MM = (totalSeconds % 3600) / 60;
    long SS = totalSeconds % 60;

    return String.format("%02d:%02d:%02d", HH, MM, SS);
}


private double parsePercentage(String percentageStr) {
    if (percentageStr == null || percentageStr.isEmpty()) {
        return 0.0;
    }
    try {
        // Заменяем запятую на точку и удаляем %
        String normalized = percentageStr.replace("%", "").replace(",", ".").trim();
        return Double.parseDouble(normalized);
    } catch (NumberFormatException e) {
        log.error("Некорректный формат percentagePlanPpp: {}", percentageStr);
        return 0.0;
    }
}

    // Helper method to parse time string in "HH:mm:ss" format to seconds
private long parseTimeToSeconds(String time) {
    if ("Нет данных".equals(time)) {
        return -1;
    }
    try {
        String[] parts = time.split(":");
        long HH = Long.parseLong(parts[0]);
        long MM = Long.parseLong(parts[1]);
        long SS = Long.parseLong(parts[2]);

        return HH * 3600 + MM * 60 + SS;
    } catch (Exception e) {
        // Обработка ошибок при парсинге времени
        System.err.println("Ошибка при парсинге времени: " + time);
        return -1; // Возвращаем -1 в случае ошибки
    }

}
    //Вспомогательный метод для безопасного преобразования String в double
private double parseDouble(String value) {
    try {
        return Double.parseDouble(value);
    } catch (NumberFormatException e) {
        System.err.println("Ошибка преобразования числа: " + value + ". Установлено значение по умолчанию 0.0");
        return 0.0;
    }

}
// Метод для преобразования секунд в формат "HH:mm:ss"
private String formatSecondsToTime(long totalSeconds) {
    long hours = totalSeconds / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;

    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
}
}