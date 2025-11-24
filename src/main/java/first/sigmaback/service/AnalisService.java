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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AnalisService {

    private final PppRepository pppRepository;
    private final AnalisHeaderService analisHeaderService;
    private final OperationService operationService;
    private final TimeService timeService;
    private final ProblemService problemService;
    private final DatesService datesService;
    private final EmployeesService employeesService;
    private final InterService interService;

    @Autowired
    public AnalisService(PppRepository pppRepository, AnalisHeaderService analisHeaderService, 
                        OperationService operationService, TimeService timeService, 
                        ProblemService problemService, DatesService datesService, 
                        EmployeesService employeesService, InterService interService) {
        this.pppRepository = pppRepository;
        this.analisHeaderService = analisHeaderService;
        this.operationService = operationService;
        this.timeService = timeService;
        this.problemService = problemService;
        this.datesService = datesService;
        this.employeesService = employeesService;
        this.interService = interService;
    }

    public AnalisFullDTO getAllTransactions() {
        log.info("Starting getAllTransactions()");
        long startTime = System.currentTimeMillis();

        // 1. Получаем данные для заголовка
        log.debug("Getting header norms");
        AnalisHeaderDTO header = analisHeaderService.getNorms();
        
        // 2. Получаем и преобразуем транзакции
        log.debug("Getting all PPP transactions from repository");
        List<Ppp> ppps = pppRepository.findAll();
        List<AnalisDTO> transactions = ppps.stream()
                .map(this::convertToAnalisDTO)
                .collect(Collectors.toList());

        // 3. Считаем статистику по выполнению плана
        log.debug("Calculating overfulfilled and underfulfilled transactions");
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

        // 4. Обновляем заголовок
        header.setOverfulfilledTransactionsCount(overfulfilledCount);
        header.setUnderfulfilledTransactionsCount(underfulfilledCount);

        // 5. Возвращаем результат
        AnalisFullDTO fullDTO = new AnalisFullDTO();
        fullDTO.setHeader(header);
        fullDTO.setTransactions(transactions);

        long endTime = System.currentTimeMillis();
        log.info("Finished getAllTransactions() in {} ms. Processed {} transactions", 
                (endTime - startTime), transactions.size());
        
        return fullDTO;
    }

    private AnalisDTO convertToAnalisDTO(Ppp ppp) {
        // ДОБАВЛЯЕМ ПРОВЕРКУ НА NULL ДЛЯ TRANSACTION
        if (ppp.getTransaction() == null) {
            log.warn("Transaction ID is null, skipping conversion");
            return new AnalisDTO(); // возвращаем пустой DTO
        }
        
        log.debug("Converting PPP to AnalisDTO for transaction: {}", ppp.getTransaction());
        long startTime = System.currentTimeMillis();

        AnalisDTO dto = new AnalisDTO();
        dto.setTransaction(ppp.getTransaction());
        dto.setStatus(ppp.getStatus());

        // Устанавливаем даты КАК String с временной зоной Екатеринбурга
        dto.setPlanDateStart(convertLocalDateToString(ppp.getPlanDateStart()));
        dto.setForecastDateStart(convertLocalDateToString(ppp.getForecastDateStart()));
        dto.setFactDateStart(convertLocalDateToString(ppp.getFactDateStart()));
        dto.setPlanDateStop(convertLocalDateToString(ppp.getPlanDateStop()));
        dto.setForecastDateStop(convertLocalDateToString(ppp.getForecastDateStop()));
        dto.setFactDateStop(convertLocalDateToString(ppp.getFactDateStop()));
        dto.setPlanDateShipment(convertLocalDateToString(ppp.getPlanDateShipment()));
        dto.setForecastDateShipment(convertLocalDateToString(ppp.getForecastDateShipment()));
        dto.setFactDateShipment(convertLocalDateToString(ppp.getFactDateShipment()));

        // Получаем нормы и время работы по профессиям
        log.debug("Getting norms and work time for transaction: {}", ppp.getTransaction());
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
        log.debug("Getting operation times from TimeService for transaction: {}", ppp.getTransaction());
        Map<String, Map<String, String>> timeServiceResults = timeService.calculateOperationTimes(ppp.getTransaction());

        // Устанавливаем значения для каждой операции
        setOperationTimes(dto, timeServiceResults);

        // Суммируем время опций и время операций
        calculateTotalWorkTimes(dto, timeServiceResults);

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
        log.debug("Total operations work time: {}", totalOperationsWorkTime);

        // Получаем и устанавливаем время проблем
        log.debug("Getting problem hours for transaction: {}", ppp.getTransaction());
        setProblemHours(dto, ppp.getTransaction());

        // Рассчитываем превышения времени для всех операций
        log.debug("Calculating time exceeded for all operations");
        calculateTimeExceeded(dto, results);

        // Рассчитываем плановые и фактические даты
        log.debug("Calculating planned and actual dates");
        calculateDates(dto, ppp, results);

        // Получаем сотрудников для операций
        log.debug("Getting employees for transaction: {}", ppp.getTransaction());
        setEmployees(dto, ppp.getTransaction());

        // Рассчитываем время между операциями
        log.debug("Calculating time between operations");
        calculateTimeBetweenOperations(dto, ppp.getTransaction());

        // Рассчитываем общее время и проценты
        log.debug("Calculating total time and percentages");
        calculateTotalTimeAndPercentage(dto, timeServiceResults);

        long endTime = System.currentTimeMillis();
        log.debug("Finished converting PPP to AnalisDTO for transaction: {} in {} ms", 
                ppp.getTransaction(), (endTime - startTime));
        
        return dto;
    }

    // ОБНОВЛЕННЫЙ МЕТОД ДЛЯ КОНВЕРТАЦИИ LocalDate В String С ВРЕМЕННОЙ ЗОНОЙ ЕКАТЕРИНБУРГА
    private String convertLocalDateToString(LocalDate date) {
        if (date == null) {
            return "Нет данных";
        }
        try {
            // Явно указываем временную зону Екатеринбурга (UTC+5)
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T00:00:00+05:00";
        } catch (Exception e) {
            log.error("Ошибка при форматировании даты: {}", date, e);
            return "Нет данных";
        }
    }

    // ОБНОВЛЕННЫЙ МЕТОД ДЛЯ КОНВЕРТАЦИИ LocalDate ИЗ DatesService В String С ВРЕМЕННОЙ ЗОНОЙ
    private String convertCalculatedDate(LocalDate date) {
        if (date == null) {
            return "Нет данных";
        }
        try {
            // Явно указываем временную зону Екатеринбурга (UTC+5)
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T00:00:00+05:00";
        } catch (Exception e) {
            log.error("Ошибка при форматировании рассчитанной даты: {}", date, e);
            return "Нет данных";
        }
    }

    private void setOperationTimes(AnalisDTO dto, Map<String, Map<String, String>> timeServiceResults) {
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
    }

    private void calculateTotalWorkTimes(AnalisDTO dto, Map<String, Map<String, String>> timeServiceResults) {
        String mechanicWorkTime = getOperationValue(timeServiceResults, "Проверка механиком", "workTime");
        long mechanicOptionSeconds = parseTimeToSeconds(dto.getMechanicOptionWorktype());
        long mechanicWorkSeconds = parseTimeToSeconds(mechanicWorkTime);

        if (mechanicOptionSeconds == -1 || mechanicWorkSeconds == -1) {
            dto.setMechanicTotalWorktime("Нет данных");
            log.debug("Mechanic total worktime: No data (option: {}, work: {})", 
                    dto.getMechanicOptionWorktype(), mechanicWorkTime);
        } else {
            dto.setMechanicTotalWorktime(sumWorkTimes(dto.getMechanicOptionWorktype(), mechanicWorkTime));
            log.debug("Mechanic total worktime calculated: {}", dto.getMechanicTotalWorktime());
        }

        String electronWorkTime = getOperationValue(timeServiceResults, "Проверка электронщиком", "workTime");
        long electronOptionSeconds = parseTimeToSeconds(dto.getElectronOptionWorktype());
        long electronWorkSeconds = parseTimeToSeconds(electronWorkTime);

        if (electronOptionSeconds == -1 || electronWorkSeconds == -1) {
            dto.setElectronTotalWorktime("Нет данных");
            log.debug("Electron total worktime: No data (option: {}, work: {})", 
                    dto.getElectronOptionWorktype(), electronWorkTime);
        } else {
            dto.setElectronTotalWorktime(sumWorkTimes(dto.getElectronOptionWorktype(), electronWorkTime));
            log.debug("Electron total worktime calculated: {}", dto.getElectronTotalWorktime());
        }

        String electricWorkTime = getOperationValue(timeServiceResults, "Подключение", "workTime");
        long electricOptionSeconds = parseTimeToSeconds(dto.getElectricOptionWorktype());
        long electricWorkSeconds = parseTimeToSeconds(electricWorkTime);

        if (electricOptionSeconds == -1 || electricWorkSeconds == -1) {
            dto.setElectricTotalWorktime("Нет данных");
            log.debug("Electric total worktime: No data (option: {}, work: {})", 
                    dto.getElectricOptionWorktype(), electricWorkTime);
        } else {
            dto.setElectricTotalWorktime(sumWorkTimes(dto.getElectricOptionWorktype(), electricWorkTime));
            log.debug("Electric total worktime calculated: {}", dto.getElectricTotalWorktime());
        }

        String techWorkTime = getOperationValue(timeServiceResults, "Проверка технологом", "workTime");
        long techOptionSeconds = parseTimeToSeconds(dto.getTechOptionWorktype());
        long techWorkSeconds = parseTimeToSeconds(techWorkTime);

        if (techOptionSeconds == -1 || techWorkSeconds == -1) {
            dto.setTechTotalWorktime("Нет данных");
            log.debug("Tech total worktime: No data (option: {}, work: {})", 
                    dto.getTechOptionWorktype(), techWorkTime);
        } else {
            dto.setTechTotalWorktime(sumWorkTimes(dto.getTechOptionWorktype(), techWorkTime));
            log.debug("Tech total worktime calculated: {}", dto.getTechTotalWorktime());
        }
    }

    private void setProblemHours(AnalisDTO dto, String transactionId) {
        Map<String, Double> problemHoursByProfession = problemService.getProblemHoursByProfession(transactionId);

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
        log.debug("Total problem hours for transaction {}: {}", transactionId, totalProblemHours);
    }

    private void calculateTimeExceeded(AnalisDTO dto, Map<String, String> results) {
        // Входной контроль
        calculateVhodControlTimeExceeded(dto);
        
        // Электрик
        calculateElectricTimeExceeded(dto, results);
        
        // Механик
        calculateMechanicTimeExceeded(dto, results);
        
        // Электронщик
        calculateElectronTimeExceeded(dto, results);
        
        // Технолог
        calculateTechTimeExceeded(dto, results);
        
        // Выходной контроль
        calculateVihodControlTimeExceeded(dto);
        
        // Транспортное положение
        calculateTransportTimeExceeded(dto);
    }

    private void calculateVhodControlTimeExceeded(AnalisDTO dto) {
        String vhodNormString = analisHeaderService.getNorms().getVhodNorm();
        double vhodNorm;
        try {
            vhodNorm = Double.parseDouble(vhodNormString);
        } catch (NumberFormatException e) {
            log.error("Ошибка: Невозможно преобразовать vhodNorm в число: {}. Установлено значение по умолчанию 0.0", vhodNormString);
            vhodNorm = 0.0;
        }

        long vhodControlWorkTimeSeconds = parseTimeToSeconds(dto.getVhodControlWorkTime());
        long vhodNormSeconds = (long) (vhodNorm * 3600);

        String vhodControlTimeExceeded;
        if (vhodControlWorkTimeSeconds == -1) {
            vhodControlTimeExceeded = "Нет данных";
        } else if (vhodControlWorkTimeSeconds >= 0 && vhodControlWorkTimeSeconds <= 300) {
            vhodControlTimeExceeded = "Контроль руководителя";
        } else if (vhodControlWorkTimeSeconds == 0) {
            vhodControlTimeExceeded = "100.00%";
        } else {
            double percentageExceeded = (vhodControlWorkTimeSeconds / (double) vhodNormSeconds) * 100;
            vhodControlTimeExceeded = String.format("%.2f%%", percentageExceeded);
        }
        dto.setVhodControlTimeExceeded(vhodControlTimeExceeded);
        log.debug("Vhod control time exceeded: {}", vhodControlTimeExceeded);
    }

    private void calculateElectricTimeExceeded(AnalisDTO dto, Map<String, String> results) {
        String podklyuchenieNormString = analisHeaderService.getNorms().getPodklyuchenieNorm();
        String electricNormString = results.get("Электрик");
        double podklyuchenieNorm;
        double electricNormFull;

        try {
            podklyuchenieNorm = Double.parseDouble(podklyuchenieNormString);
        } catch (NumberFormatException e) {
            log.error("Ошибка: Невозможно преобразовать podklyuchenieNorm в число: {}. Установлено значение по умолчанию 0.0", podklyuchenieNormString);
            podklyuchenieNorm = 0.0;
        }

        try {
            electricNormFull = Double.parseDouble(electricNormString);
        } catch (NumberFormatException e) {
            log.error("Ошибка: Невозможно преобразовать electricNormFull в число: {}. Установлено значение по умолчанию 0.0", electricNormString);
            electricNormFull = 0.0;
        }

        long electricTotalWorkTimeSeconds = parseTimeToSeconds(dto.getElectricTotalWorktime());
        double electricProblemHours = dto.getElectricProblemHours();
        long electricProblemHoursSeconds = (long) (electricProblemHours * 3600);

        long cleanElectricTimeSeconds = (electricTotalWorkTimeSeconds == -1 || electricProblemHoursSeconds == -1) ? -1 : electricTotalWorkTimeSeconds - electricProblemHoursSeconds;

        double totalElectricNorm = podklyuchenieNorm + electricNormFull;
        long totalElectricNormSeconds = (long) (totalElectricNorm * 3600);

        String electricTimeExceeded;
        if (cleanElectricTimeSeconds == -1) {
            electricTimeExceeded = "Нет данных";
        } else if (cleanElectricTimeSeconds >= 0 && cleanElectricTimeSeconds <= 300) {
            electricTimeExceeded = "Контроль руководителя";
        } else if (cleanElectricTimeSeconds == 0) {
            electricTimeExceeded = "100.00%";
        } else {
            double percentageExceeded = (cleanElectricTimeSeconds / (double) totalElectricNormSeconds) * 100;
            electricTimeExceeded = String.format("%.2f%%", percentageExceeded);
        }
        dto.setElectricTimeExceeded(electricTimeExceeded);
        log.debug("Electric time exceeded: {}", electricTimeExceeded);
    }

    private void calculateMechanicTimeExceeded(AnalisDTO dto, Map<String, String> results) {
        String mechOperationNormString = analisHeaderService.getNorms().getMechOperationNorm();
        String mechNormString = results.get("Механик");
        double mechOperationNorm;
        double mechNormFull;

        try {
            mechOperationNorm = Double.parseDouble(mechOperationNormString);
        } catch (NumberFormatException e) {
            log.error("Ошибка: Невозможно преобразовать mechOperationNorm в число: {}. Установлено значение по умолчанию 0.0", mechOperationNormString);
            mechOperationNorm = 0.0;
        }

        try {
            mechNormFull = Double.parseDouble(mechNormString);
        } catch (NumberFormatException e) {
            log.error("Ошибка: Невозможно преобразовать mechNormFull в число: {}. Установлено значение по умолчанию 0.0", mechNormString);
            mechNormFull = 0.0;
        }

        long mechTotalWorkTimeSeconds = parseTimeToSeconds(dto.getMechanicTotalWorktime());
        double mechProblemHours = dto.getMechanicProblemHours();
        long mechProblemHoursSeconds = (long) (mechProblemHours * 3600);

        long cleanmechTimeSeconds = (mechTotalWorkTimeSeconds == -1 || mechProblemHoursSeconds == -1) ? -1 : mechTotalWorkTimeSeconds - mechProblemHoursSeconds;

        double totalmechNorm = mechOperationNorm + mechNormFull;
        long totalmechNormSeconds = (long) (totalmechNorm * 3600);

        String mechTimeExceeded;
        if (cleanmechTimeSeconds == -1) {
            mechTimeExceeded = "Нет данных";
        } else if (cleanmechTimeSeconds >= 0 && cleanmechTimeSeconds <= 300) {
            mechTimeExceeded = "Контроль руководителя";
        } else if (cleanmechTimeSeconds == 0) {
            mechTimeExceeded = "100.00%";
        } else {
            double percentageExceeded = (cleanmechTimeSeconds / (double) totalmechNormSeconds) * 100;
            mechTimeExceeded = String.format("%.2f%%", percentageExceeded);
        }
        dto.setMechanicTimeExceeded(mechTimeExceeded);
        log.debug("Mechanic time exceeded: {}", mechTimeExceeded);
    }

    private void calculateElectronTimeExceeded(AnalisDTO dto, Map<String, String> results) {
        String electronOperationNormString = analisHeaderService.getNorms().getElectronOperationNorm();
        String electronNormString = results.get("Электронщик");
        double electronOperationNorm;
        double electronNormFull;

        try {
            electronOperationNorm = Double.parseDouble(electronOperationNormString);
        } catch (NumberFormatException e) {
            log.error("Ошибка: Невозможно преобразовать electronOperationNorm в число: {}. Установлено значение по умолчанию 0.0", electronOperationNormString);
            electronOperationNorm = 0.0;
        }

        try {
            electronNormFull = Double.parseDouble(electronNormString);
        } catch (NumberFormatException e) {
            log.error("Ошибка: Невозможно преобразовать electronNormFull в число: {}. Установлено значение по умолчанию 0.0", electronNormString);
            electronNormFull = 0.0;
        }

        long electronTotalWorkTimeSeconds = parseTimeToSeconds(dto.getElectronTotalWorktime());
        double electronProblemHours = dto.getElectronProblemHours();
        long electronProblemHoursSeconds = (long) (electronProblemHours * 3600);

        long cleanElectronTimeSeconds = (electronTotalWorkTimeSeconds == -1 || electronProblemHoursSeconds == -1) ? -1 : electronTotalWorkTimeSeconds - electronProblemHoursSeconds;

        double totalElectronNorm = electronOperationNorm + electronNormFull;
        long totalElectronNormSeconds = (long) (totalElectronNorm * 3600);

        String electronTimeExceeded;
        if (cleanElectronTimeSeconds == -1) {
            electronTimeExceeded = "Нет данных";
        } else if (cleanElectronTimeSeconds >= 0 && cleanElectronTimeSeconds <= 300) {
            electronTimeExceeded = "Контроль руководителя";
        } else if (cleanElectronTimeSeconds == 0) {
            electronTimeExceeded = "100.00%";
        } else {
            double percentageExceeded = (cleanElectronTimeSeconds / (double) totalElectronNormSeconds) * 100;
            electronTimeExceeded = String.format("%.2f%%", percentageExceeded);
        }
        dto.setElectronTimeExceeded(electronTimeExceeded);
        log.debug("Electron time exceeded: {}", electronTimeExceeded);
    }

    private void calculateTechTimeExceeded(AnalisDTO dto, Map<String, String> results) {
        String techOperationNormString = analisHeaderService.getNorms().getTechOperationNorm();
        String techNormString = results.get("Технолог");
        double techOperationNorm;
        double techNormFull;

        try {
            techOperationNorm = Double.parseDouble(techOperationNormString);
        } catch (NumberFormatException e) {
            log.error("Ошибка: Невозможно преобразовать techOperationNorm в число: {}. Установлено значение по умолчанию 0.0", techOperationNormString);
            techOperationNorm = 0.0;
        }

        try {
            techNormFull = Double.parseDouble(techNormString);
        } catch (NumberFormatException e) {
            log.error("Ошибка: Невозможно преобразовать techNormFull в число: {}. Установлено значение по умолчанию 0.0", techNormString);
            techNormFull = 0.0;
        }

        long techTotalWorkTimeSeconds = parseTimeToSeconds(dto.getTechTotalWorktime());
        double techProblemHours = dto.getTechProblemHours();
        long techProblemHoursSeconds = (long) (techProblemHours * 3600);

        long cleantechTimeSeconds = (techTotalWorkTimeSeconds == -1 || techProblemHoursSeconds == -1) ? -1 : techTotalWorkTimeSeconds - techProblemHoursSeconds;

        double totaltechNorm = techOperationNorm + techNormFull;
        long totaltechNormSeconds = (long) (totaltechNorm * 3600);

        String techTimeExceeded;
        if (cleantechTimeSeconds == -1) {
            techTimeExceeded = "Нет данных";
        } else if (cleantechTimeSeconds >= 0 && cleantechTimeSeconds <= 300) {
            techTimeExceeded = "Контроль руководителя";
        } else if (cleantechTimeSeconds == 0) {
            techTimeExceeded = "100.00%";
        } else {
            double percentageExceeded = (cleantechTimeSeconds / (double) totaltechNormSeconds) * 100;
            techTimeExceeded = String.format("%.2f%%", percentageExceeded);
        }
        dto.setTechTimeExceeded(techTimeExceeded);
        log.debug("Tech time exceeded: {}", techTimeExceeded);
    }

    private void calculateVihodControlTimeExceeded(AnalisDTO dto) {
        String vihodNormString = analisHeaderService.getNorms().getVihodNorm();
        double vihodNorm;
        try {
            vihodNorm = Double.parseDouble(vihodNormString);
        } catch (NumberFormatException e) {
            log.error("Ошибка: Невозможно преобразовать vihodNorm в число: {}. Установлено значение по умолчанию 0.0", vihodNormString);
            vihodNorm = 0.0;
        }

        long vihodControlWorkTimeSeconds = parseTimeToSeconds(dto.getVihodControlWorkTime());
        long vihodNormSeconds = (long) (vihodNorm * 3600);

        String vihodControlTimeExceeded;
        if (vihodControlWorkTimeSeconds == -1 || vihodControlWorkTimeSeconds == 0) {
            vihodControlTimeExceeded = "Нет данных";
        } else if (vihodControlWorkTimeSeconds >= 0 && vihodControlWorkTimeSeconds <= 300) {
            vihodControlTimeExceeded = "Контроль руководителя";
        } else {
            double percentageExceeded = (vihodControlWorkTimeSeconds / (double) vihodNormSeconds) * 100;
            vihodControlTimeExceeded = String.format("%.2f%%", percentageExceeded);
        }
        dto.setVihodControlTimeExceeded(vihodControlTimeExceeded);
        log.debug("Vihod control time exceeded: {}", vihodControlTimeExceeded);
    }

    private void calculateTransportTimeExceeded(AnalisDTO dto) {
        String transportNormString = analisHeaderService.getNorms().getTransportNorm();
        double transportNorm;
        try {
            transportNorm = Double.parseDouble(transportNormString);
        } catch (NumberFormatException e) {
            log.error("Ошибка: Невозможно преобразовать transportNorm в число: {}. Установлено значение по умолчанию 0.0", transportNormString);
            transportNorm = 0.0;
        }

        long transportWorkTimeSeconds = parseTimeToSeconds(dto.getTransportPolozhenieWorkTime());
        long transportNormSeconds = (long) (transportNorm * 3600);

        String transportTimeExceeded;
        if (transportWorkTimeSeconds == -1 || transportNormSeconds == 0) {
            transportTimeExceeded = "Нет данных";
        } else if (transportWorkTimeSeconds >= 0 && transportWorkTimeSeconds <= 300) {
            transportTimeExceeded = "Контроль руководителя";
        } else {
            double percentageExceeded = (transportWorkTimeSeconds / (double) transportNormSeconds) * 100;
            transportTimeExceeded = String.format("%.2f%%", percentageExceeded);
        }
        dto.setTransportTimeExceeded(transportTimeExceeded);
        log.debug("Transport time exceeded: {}", transportTimeExceeded);
    }

    private void calculateDates(AnalisDTO dto, Ppp ppp, Map<String, String> results) {
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

        // ВЫЗОВ DATESERVICE С ПРОВЕРКОЙ НА NULL
        Map<Integer, LocalDate> calculatedDates = datesService.calculateDates(planDateStart, norms);

        // Устанавливаем даты в DTO КАК String с временной зоной
        dto.setPlanDate1(calculatedDates.containsKey(1) ? convertCalculatedDate(calculatedDates.get(1)) : "Нет данных");
        dto.setPlanDate2(calculatedDates.containsKey(2) ? convertCalculatedDate(calculatedDates.get(2)) : "Нет данных");
        dto.setPlanDate3(calculatedDates.containsKey(3) ? convertCalculatedDate(calculatedDates.get(3)) : "Нет данных");
        dto.setPlanDate4(calculatedDates.containsKey(4) ? convertCalculatedDate(calculatedDates.get(4)) : "Нет данных");
        dto.setPlanDate5(calculatedDates.containsKey(5) ? convertCalculatedDate(calculatedDates.get(5)) : "Нет данных");
        dto.setPlanDate6(calculatedDates.containsKey(6) ? convertCalculatedDate(calculatedDates.get(6)) : "Нет данных");
        dto.setPlanDate7(calculatedDates.containsKey(7) ? convertCalculatedDate(calculatedDates.get(7)) : "Нет данных");

        // Фактические даты
        LocalDate startDate;
        if (ppp.getFactDateStart() != null) {
            startDate = ppp.getFactDateStart();
        } else if (ppp.getForecastDateStart() != null) {
            startDate = ppp.getForecastDateStart();
        } else {
            if (ppp.getPlanDateStart() != null) {
                startDate = ppp.getPlanDateStart();
            } else {
                // Устанавливаем "Нет данных" для всех фактических дат
                dto.setFactDate1("Нет данных");
                dto.setFactDate2("Нет данных");
                dto.setFactDate3("Нет данных");
                dto.setFactDate4("Нет данных");
                dto.setFactDate5("Нет данных");
                dto.setFactDate6("Нет данных");
                dto.setFactDate7("Нет данных");
                return;
            }
        }

        // Создаем карту с нормативами для фактических дат
        Map<Integer, Double> norms2 = new HashMap<>();

        norms2.put(1, parseDouble(analisHeaderService.getNorms().getVhodNorm()));
        norms2.put(2, parseDouble(analisHeaderService.getNorms().getPodklyuchenieNorm()) + parseDouble(results.get("Электрик")));
        norms2.put(3, parseDouble(analisHeaderService.getNorms().getMechOperationNorm()) + parseDouble(results.get("Механик")));
        norms2.put(4, parseDouble(analisHeaderService.getNorms().getElectronOperationNorm()) + parseDouble(results.get("Электронщик")));
        norms2.put(5, parseDouble(analisHeaderService.getNorms().getTechOperationNorm()) + parseDouble(results.get("Технолог")));
        norms2.put(6, parseDouble(analisHeaderService.getNorms().getVihodNorm()));
        norms2.put(7, parseDouble(analisHeaderService.getNorms().getTransportNorm()));

        // ВЫЗОВ DATESERVICE ДЛЯ ФАКТИЧЕСКИХ ДАТ
        Map<Integer, LocalDate> calculatedDates2 = datesService.calculateDates(startDate, norms2);

        // Устанавливаем фактические даты в DTO КАК String с временной зоной
        dto.setFactDate1(calculatedDates2.containsKey(1) ? convertCalculatedDate(calculatedDates2.get(1)) : "Нет данных");
        dto.setFactDate2(calculatedDates2.containsKey(2) ? convertCalculatedDate(calculatedDates2.get(2)) : "Нет данных");
        dto.setFactDate3(calculatedDates2.containsKey(3) ? convertCalculatedDate(calculatedDates2.get(3)) : "Нет данных");
        dto.setFactDate4(calculatedDates2.containsKey(4) ? convertCalculatedDate(calculatedDates2.get(4)) : "Нет данных");
        dto.setFactDate5(calculatedDates2.containsKey(5) ? convertCalculatedDate(calculatedDates2.get(5)) : "Нет данных");
        dto.setFactDate6(calculatedDates2.containsKey(6) ? convertCalculatedDate(calculatedDates2.get(6)) : "Нет данных");
        dto.setFactDate7(calculatedDates2.containsKey(7) ? convertCalculatedDate(calculatedDates2.get(7)) : "Нет данных");

        log.debug("Dates calculated for transaction: {}", ppp.getTransaction());
    }

    private void setEmployees(AnalisDTO dto, String transactionId) {
        Map<String, String> employeeNames = employeesService.getEmployeeNamesForTransaction(transactionId);

        // Устанавливаем фамилии в DTO
        dto.setVhodControlEmployee(employeeNames.get("Входной контроль"));
        dto.setPodkluchenieEmployee(employeeNames.get("Подключение"));
        dto.setProverkaMehanikomEmployee(employeeNames.get("Проверка механиком"));
        dto.setProverkaElectronEmployee(employeeNames.get("Проверка электронщиком"));
        dto.setProverkaTehnologomEmployee(employeeNames.get("Проверка технологом"));
        dto.setVihodControlEmployee(employeeNames.get("Выходной контроль"));
        dto.setTransportPolozhenieEmployee(employeeNames.get("Транспортное положение"));

        log.debug("Employees set for transaction: {}", transactionId);
    }

    private void calculateTimeBetweenOperations(AnalisDTO dto, String transactionId) {
        Map<String, Map<String, String>> timeServiceResults2 = timeService.calculateOperationTimes(transactionId);

        // Получаем startTime и stopTime для операций
        String vhodControlStopTime = getOperationValue(timeServiceResults2, "Входной контроль", "stopTime");
        String podkluchenieStartTime = getOperationValue(timeServiceResults2, "Подключение", "startTime");
        String podkluchenieStopTime = getOperationValue(timeServiceResults2, "Подключение", "stopTime");
        String proverkaMehanikomStartTime = getOperationValue(timeServiceResults2, "Проверка механиком", "startTime");
        String proverkaMehanikomStopTime = getOperationValue(timeServiceResults2, "Проверка механиком", "stopTime");
        String proverkaElectronStartTime = getOperationValue(timeServiceResults2, "Проверка электронщиком", "startTime");
        String proverkaElectronStopTime = getOperationValue(timeServiceResults2, "Проверка электронщиком", "stopTime");
        String proverkaTehnologomStartTime = getOperationValue(timeServiceResults2, "Проверка технологом", "startTime");
        String proverkaTehnologomStopTime = getOperationValue(timeServiceResults2, "Проверка технологом", "stopTime");
        String vihodControlStartTime = getOperationValue(timeServiceResults2, "Выходной контроль", "startTime");
        String vihodControlStopTime = getOperationValue(timeServiceResults2, "Выходной контроль", "stopTime");
        String transportPolozhenieStartTime = getOperationValue(timeServiceResults2, "Транспортное положение", "startTime");

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

        // Суммируем все времена
        String totalTime = sumWorkTimes2(
                dto.getTimeBetweenVhodAndPodkluchenie(),
                dto.getTimeBetweenPodkluchenieAndProverkaMehanikom(),
                dto.getTimeBetweenProverkaMehanikomAndProverkaElectron(),
                dto.getTimeBetweenProverkaElectronAndProverkaTehnologom(),
                dto.getTimeBetweenProverkaTehnologomAndVihodControl(),
                dto.getTimeBetweenVihodControlAndTransportPolozhenie()
        );

        // Устанавливаем общую сумму в DTO
        dto.setTotalTimeBetweenOperations(totalTime);
        log.debug("Time between operations calculated for transaction: {}", transactionId);
    }

    private void calculateTotalTimeAndPercentage(AnalisDTO dto, Map<String, Map<String, String>> timeServiceResults) {
        // Получаем значения из DTO
        String totalOperationsWorkTime2 = dto.getTotalOperationsWorkTime();
        double problemHours = dto.getTotalProblemHours();
        String timeBetweenOperations = dto.getTotalTimeBetweenOperations();

        // Преобразуем все значения в секунды
        long workTimeInSeconds = parseTimeToSeconds(totalOperationsWorkTime2);
        if (workTimeInSeconds == -1) {
            workTimeInSeconds = 0;
        }

        long problemTimeInSeconds = (long) (problemHours * 3600);
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

        // Расчет процента выполнения плана
        int totalNormsAndProblems = (int)(dto.getTotalProfessionNorms() + analisHeaderService.getTotalHeaderNorms());
        dto.setPlanPpp(totalNormsAndProblems);
        int planPppMinutes = dto.getPlanPpp();

        // Преобразуем planPpp в секунды
        int planPppSeconds = planPppMinutes * 3600;

        // Вычисляем totalOperationsWorkTime в секундах
        long totalOperationsWorkTimeSeconds = 0;
        for (Map.Entry<String, Map<String, String>> entry : timeServiceResults.entrySet()) {
            String operationName = entry.getKey();
            Map<String, String> operationInfo = entry.getValue();
            String workTime = operationInfo.get("workTime");
            long workTimeInSeconds2 = parseTimeToSeconds(workTime);
            if (workTimeInSeconds2 != -1) {
                totalOperationsWorkTimeSeconds += workTimeInSeconds2;
            }
        }

        // Вычисляем процент
        double percentage;
        if (totalOperationsWorkTimeSeconds == 0) {
            percentage = 0;
        } else {
            percentage = totalOperationsWorkTimeSeconds * 100 / (double) (planPppSeconds);
        }

        // Форматируем результат
        String formattedPercentage = String.format("%.2f%%", percentage);

        // Устанавливаем результат в DTO
        dto.setPercentagePlanPpp(formattedPercentage);
        
        log.debug("Total time and percentage calculated: totalTime={}, percentage={}", formattedTotalTime, formattedPercentage);
    }

    private String getOperationValue(Map<String, Map<String, String>> timeServiceResults, String operationName, String valueName) {
        if (timeServiceResults.containsKey(operationName) && timeServiceResults.get(operationName).containsKey(valueName)) {
            return timeServiceResults.get(operationName).get(valueName);
        }
        log.debug("Operation value not found: {} -> {}", operationName, valueName);
        return "00:00:00";
    }

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
            log.debug("Both times are 'No data': {} and {}", time1, time2);
            return "Нет данных";
        }

        long HH = totalSeconds / 3600;
        long MM = (totalSeconds % 3600) / 60;
        long SS = totalSeconds % 60;

        String result = String.format("%02d:%02d:%02d", HH, MM, SS);
        log.debug("Sum work times: {} + {} = {}", time1, time2, result);
        return result;
    }

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
            log.debug("All times are 'No data'");
            return "Нет данных";
        }

        long HH = totalSeconds / 3600;
        long MM = (totalSeconds % 3600) / 60;
        long SS = totalSeconds % 60;

        String result = String.format("%02d:%02d:%02d", HH, MM, SS);
        log.debug("Sum work times 2: result = {}", result);
        return result;
    }

    private double parsePercentage(String percentageStr) {
        if (percentageStr == null || percentageStr.isEmpty()) {
            log.warn("Percentage string is null or empty");
            return 0.0;
        }
        try {
            String normalized = percentageStr.replace("%", "").replace(",", ".").trim();
            double result = Double.parseDouble(normalized);
            log.debug("Parsed percentage: {} -> {}", percentageStr, result);
            return result;
        } catch (NumberFormatException e) {
            log.error("Некорректный формат percentagePlanPpp: {}", percentageStr, e);
            return 0.0;
        }
    }

    private long parseTimeToSeconds(String time) {
        if ("Нет данных".equals(time)) {
            return -1;
        }
        try {
            String[] parts = time.split(":");
            long HH = Long.parseLong(parts[0]);
            long MM = Long.parseLong(parts[1]);
            long SS = Long.parseLong(parts[2]);

            long result = HH * 3600 + MM * 60 + SS;
            log.debug("Parsed time to seconds: {} -> {} seconds", time, result);
            return result;
        } catch (Exception e) {
            log.error("Ошибка при парсинге времени: {}", time, e);
            return -1;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.error("Ошибка преобразования числа: {}. Установлено значение по умолчанию 0.0", value, e);
            return 0.0;
        }
    }

    private String formatSecondsToTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String result = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        log.debug("Formatted seconds to time: {} seconds -> {}", totalSeconds, result);
        return result;
    }
}