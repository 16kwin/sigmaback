package first.sigmaback.service;

import first.sigmaback.dto.EmployeesDto;
import first.sigmaback.entity.Employees;
import first.sigmaback.repository.EmployeesRepository;
import first.sigmaback.repository.TimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import first.sigmaback.dto.AnalisFullDTO;
import first.sigmaback.dto.AnalisDTO;
import first.sigmaback.dto.AnalisHeaderDTO;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import first.sigmaback.entity.DataCache;
import first.sigmaback.repository.DataCacheRepository;
import java.util.Optional;

@Service
public class EmployeesTableService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeesTableService.class);

    private final EmployeesRepository employeesRepository;
    private final DataCacheRepository dataCacheRepository;
    private final ObjectMapper objectMapper;
    private final TimeRepository timeRepository;

    @Autowired
    public EmployeesTableService(EmployeesRepository employeesRepository, DataCacheRepository dataCacheRepository, ObjectMapper objectMapper, TimeRepository timeRepository) {
        this.employeesRepository = employeesRepository;
        this.dataCacheRepository = dataCacheRepository;
        this.objectMapper = objectMapper;
        this.timeRepository = timeRepository;
    }

    public List<EmployeesDto> getUniqueEmployeeNamesBySpecialization(YearMonth yearMonth) {
        logger.info("Entering getUniqueEmployeeNamesBySpecialization() with yearMonth: {}", yearMonth);
        long startTime = System.currentTimeMillis();

        // 1. Определяем список допустимых специализаций
        logger.debug("Starting: Defining allowed specializations");
        long startTimeAllowedSpecializations = System.currentTimeMillis();
        List<String> allowedSpecializations = Arrays.asList("Механик", "Электрик", "Электронщик", "Технолог", "Комплектация");
        long endTimeAllowedSpecializations = System.currentTimeMillis();
        logger.debug("Finished: Defining allowed specializations, took {} ms", (endTimeAllowedSpecializations - startTimeAllowedSpecializations));

        // 2. Получаем всех сотрудников из базы данных
        logger.debug("Starting: Getting all employees from the database");
        long startTimeFindAll = System.currentTimeMillis();
        List<Employees> employees = employeesRepository.findAll();
        long endTimeFindAll = System.currentTimeMillis();
        logger.debug("Finished: Getting all employees from the database", (endTimeFindAll - startTimeFindAll));
        logger.debug("Found {} employees in the database", employees.size());

        // 3. Фильтруем сотрудников по допустимым специализациям, извлекаем имена и специализации и удаляем дубликаты
        logger.debug("Starting: Filtering, mapping, and distincting employees");
        long startTimeStream = System.currentTimeMillis();
        List<EmployeesDto> result = employees.stream()
                .filter(employee -> {
                    boolean allowed = allowedSpecializations.contains(employee.getEmployeesSpecialization());
                    logger.trace("Employee {} specialization {} is allowed: {}", employee.getEmployeesName(), employee.getEmployeesSpecialization(), allowed);
                    return allowed;
                })
                .map(employee -> {
                    EmployeesDto dto = convertToDto(employee, yearMonth);
                     logger.trace("Employee {} converted to DTO: {}", employee.getEmployeesName(), employee.getEmployeesSpecialization(), dto);
                    return dto;
                })
                .distinct()
                .collect(Collectors.toList());
        long endTimeStream = System.currentTimeMillis();
        logger.debug("Finished: Filtering, mapping, and distincting employees", (endTimeStream - startTimeStream));

        long endTime = System.currentTimeMillis();
        logger.info("Exiting getUniqueEmployeeNamesBySpecialization() after {} ms. Result size: {}", (endTime - startTime), result.size());
        return result;
    }

     private EmployeesDto convertToDto(Employees employee, YearMonth yearMonth) {
        logger.debug("Entering convertToDto() with employee: {} and yearMonth: {}", employee.getEmployeesName(), yearMonth);
        long startTime = System.currentTimeMillis();

        EmployeesDto dto = new EmployeesDto();
        dto.setEmployeeName(employee.getEmployeesName());
        dto.setEmployeeSpecialization(employee.getEmployeesSpecialization());
         int[] transactionData = getTransactionDataForEmployee(employee.getEmployeesName(), yearMonth);
        dto.setTransactionCount(transactionData[0]);
         dto.setExceededTimeCount(transactionData[1]);

        // Set exceededOrNoOperations
        if (dto.getTransactionCount() > 0) {
            double ratio = (double) dto.getExceededTimeCount() / dto.getTransactionCount();
            dto.setExceededOrNoOperations(String.format("%.2f", ratio)); // Format to 2 decimal places
        } else {
            dto.setExceededOrNoOperations("Нет операций");
        }

        int[] normAndWorkTime = getTotalNormTimeForEmployee(employee.getEmployeesName(), yearMonth);
        dto.setTotalNormTime(normAndWorkTime[0]);
        dto.setTotalWorkTime(formatSecondsToTime(normAndWorkTime[1]));

        // Calculate workTimePercentage
        if (dto.getTotalNormTime() > 0) {
            double percentage = ((double) normAndWorkTime[1]/ dto.getTotalNormTime()/3600) * 100;
            dto.setWorkTimePercentage(String.format("%.2f", percentage).replace('.', ','));
        } else {
            dto.setWorkTimePercentage("Нет данных");
        }

        Integer hoursMounth = getHoursForYearMonth(yearMonth);
        dto.setHoursMounth(hoursMounth);

       // Calculate hoursMounthPercentage
        if (normAndWorkTime[1] > 0 && hoursMounth != null) {
            double percentage = ((double) normAndWorkTime[1]/hoursMounth/3600 ) * 100;
            dto.setHoursMounthPercentage(String.format("%.2f", percentage).replace('.', ','));
        } else {
            dto.setHoursMounthPercentage("Нет данных");
        }

        long endTime = System.currentTimeMillis();
        logger.debug("Exiting convertToDto() after {} ms", (endTime - startTime));
        return dto;
    }

public int[] getTransactionDataForEmployee(String employeeName, YearMonth yearMonth) {
    logger.debug("Получение данных по операциям для сотрудника: {}, месяц: {}", employeeName, yearMonth);
    int transactionCount = 0;
    int exceededTimeCount = 0;

    // 1. Получаем закешированные данные
    Optional<DataCache> dataCacheOptional = dataCacheRepository.findByJsonName("Первый json");
    if (!dataCacheOptional.isPresent()) {
        logger.warn("Данные не найдены в кэше");
        return new int[]{0, 0};
    }

    // 2. Парсим JSON
    AnalisFullDTO analisFullData;
    try {
        analisFullData = objectMapper.readValue(dataCacheOptional.get().getJson(), AnalisFullDTO.class);
    } catch (Exception e) {
        logger.error("Ошибка парсинга JSON", e);
        return new int[]{0, 0};
    }

    // 3. Проверяем все транзакции (без фильтрации по factDateStop)
    for (AnalisDTO transaction : analisFullData.getTransactions()) {
        // Входной контроль
        if (employeeName.equals(transaction.getVhodControlEmployee()) && 
            isValidStopTime(transaction.getVhodControlStopTime(), yearMonth)) {
            transactionCount++;
            exceededTimeCount += checkTimeExceeded(transaction.getVhodControlTimeExceeded());
        }
        
        // Подключение
        if (employeeName.equals(transaction.getPodkluchenieEmployee()) && 
            isValidStopTime(transaction.getPodkluchenieStopTime(), yearMonth)) {
            transactionCount++;
            exceededTimeCount += checkTimeExceeded(transaction.getElectricTimeExceeded());
        }
        
        // Проверка механиком
        if (employeeName.equals(transaction.getProverkaMehanikomEmployee()) && 
            isValidStopTime(transaction.getProverkaMehanikomStopTime(), yearMonth)) {
            transactionCount++;
            exceededTimeCount += checkTimeExceeded(transaction.getMechanicTimeExceeded());
        }
        
        // Проверка электронщиком
        if (employeeName.equals(transaction.getProverkaElectronEmployee()) && 
            isValidStopTime(transaction.getProverkaElectronStopTime(), yearMonth)) {
            transactionCount++;
            exceededTimeCount += checkTimeExceeded(transaction.getElectronTimeExceeded());
        }
        
        // Проверка технологом
        if (employeeName.equals(transaction.getProverkaTehnologomEmployee()) && 
            isValidStopTime(transaction.getProverkaTehnologomStopTime(), yearMonth)) {
            transactionCount++;
            exceededTimeCount += checkTimeExceeded(transaction.getTechTimeExceeded());
        }
        
        // Транспортное положение
        if (employeeName.equals(transaction.getTransportPolozhenieEmployee()) && 
            isValidStopTime(transaction.getTransportPolozhenieStopTime(), yearMonth)) {
            transactionCount++;
            exceededTimeCount += checkTimeExceeded(transaction.getTransportTimeExceeded());
        }
        
        // Выходной контроль
        if (employeeName.equals(transaction.getVihodControlEmployee()) && 
            isValidStopTime(transaction.getVihodControlStopTime(), yearMonth)) {
            transactionCount++;
            exceededTimeCount += checkTimeExceeded(transaction.getVihodControlTimeExceeded());
        }
    }

    logger.info("Итог по сотруднику {}: операций - {}, с превышением - {}", 
        employeeName, transactionCount, exceededTimeCount);
    return new int[]{transactionCount, exceededTimeCount};
}

// Проверка превышения времени (вынесено в отдельный метод для удобства)
private int checkTimeExceeded(String timeExceededValue) {
    if (timeExceededValue == null || 
        "Нет данных".equalsIgnoreCase(timeExceededValue) || 
        "Контроль руководителя".equalsIgnoreCase(timeExceededValue)) {
        return 0;
    }
    try {
        String cleanedValue = timeExceededValue.replace("%", "").replace(",", ".");
        double value = Double.parseDouble(cleanedValue);
        return value >= 100 ? 1 : 0;
    } catch (NumberFormatException e) {
        logger.error("Некорректное значение превышения времени: " + timeExceededValue, e);
        return 0;
    }
}



    private int isTimeExceeded(String timeExceeded) {
        if (timeExceeded != null && !"Нет данных".equalsIgnoreCase(timeExceeded)) {
            try {
                //Удаляем "%" и заменяем "," на "." для правильного парсинга
                String cleanedValue = timeExceeded.replace("%", "").replace(",", ".");
                double exceededValue = Double.parseDouble(cleanedValue);
                if (exceededValue >= 100) {
                    return 1;
                }
            } catch (NumberFormatException e) {
                logger.error("Error parsing TimeExceeded value: " + timeExceeded, e);
            }
        }
        return 0;
    }

  public int[] getTotalNormTimeForEmployee(String employeeName, YearMonth yearMonth) {
    logger.info("Расчёт нормативов для {} за {}", employeeName, yearMonth);
    int totalNormTime = 0; // Сумма нормативов в часах
    long totalWorkTime = 0; // Сумма фактического времени в секундах

    // 1. Получаем и парсим данные
    Optional<DataCache> dataCacheOptional = dataCacheRepository.findByJsonName("Первый json");
    if (!dataCacheOptional.isPresent() || dataCacheOptional.get().getJson() == null) {
        logger.warn("Данные не найдены в кэше");
        return new int[]{0, 0};
    }

    AnalisFullDTO analisFullData;
    try {
        analisFullData = objectMapper.readValue(dataCacheOptional.get().getJson(), AnalisFullDTO.class);
    } catch (Exception e) {
        logger.error("Ошибка парсинга JSON", e);
        return new int[]{0, 0};
    }

    // 2. Получаем нормативы
    AnalisHeaderDTO header = objectMapper.convertValue(analisFullData.getHeader(), AnalisHeaderDTO.class);

    // 3. Обрабатываем все операции
    for (AnalisDTO transaction : analisFullData.getTransactions()) {
        String transactionId = transaction.getTransaction();

        // Входной контроль
        if (employeeName.equals(transaction.getVhodControlEmployee()) && 
            isValidStopTime(transaction.getVhodControlStopTime(), yearMonth)) {
            int norm = parseNormTime(header.getVhodNorm());
            long work = parseWorkTime(transaction.getVhodControlWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.debug("{}: Входной контроль +{}ч (норматив), +{}сек (факт)", 
                transactionId, norm, work);
        }

        // Подключение
        if (employeeName.equals(transaction.getPodkluchenieEmployee()) && 
            isValidStopTime(transaction.getPodkluchenieStopTime(), yearMonth)) {
            int norm = parseNormTime(header.getPodklyuchenieNorm());
            long work = parseWorkTime(transaction.getPodkluchenieWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.debug("{}: Подключение +{}ч (норматив), +{}сек (факт)", 
                transactionId, norm, work);
        }

        // Проверка механиком
        if (employeeName.equals(transaction.getProverkaMehanikomEmployee()) && 
            isValidStopTime(transaction.getProverkaMehanikomStopTime(), yearMonth)) {
            int norm = parseNormTime(header.getMechOperationNorm());
            long work = parseWorkTime(transaction.getProverkaMehanikomWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.debug("{}: Проверка механиком +{}ч (норматив), +{}сек (факт)", 
                transactionId, norm, work);
        }

        // Проверка электронщиком
        if (employeeName.equals(transaction.getProverkaElectronEmployee()) && 
            isValidStopTime(transaction.getProverkaElectronStopTime(), yearMonth)) {
            int norm = parseNormTime(header.getElectronOperationNorm());
            long work = parseWorkTime(transaction.getProverkaElectronWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.debug("{}: Проверка электронщиком +{}ч (норматив), +{}сек (факт)", 
                transactionId, norm, work);
        }

        // Проверка технологом
        if (employeeName.equals(transaction.getProverkaTehnologomEmployee()) && 
            isValidStopTime(transaction.getProverkaTehnologomStopTime(), yearMonth)) {
            int norm = parseNormTime(header.getTechOperationNorm());
            long work = parseWorkTime(transaction.getProverkaTehnologomWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.debug("{}: Проверка технологом +{}ч (норматив), +{}сек (факт)", 
                transactionId, norm, work);
        }

        // Транспортное положение
        if (employeeName.equals(transaction.getTransportPolozhenieEmployee()) && 
            isValidStopTime(transaction.getTransportPolozhenieStopTime(), yearMonth)) {
            int norm = parseNormTime(header.getTransportNorm());
            long work = parseWorkTime(transaction.getTransportPolozhenieWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.debug("{}: Транспортное положение +{}ч (норматив), +{}сек (факт)", 
                transactionId, norm, work);
        }

        // Выходной контроль
        if (employeeName.equals(transaction.getVihodControlEmployee()) && 
            isValidStopTime(transaction.getVihodControlStopTime(), yearMonth)) {
            int norm = parseNormTime(header.getVihodNorm());
            long work = parseWorkTime(transaction.getVihodControlWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.debug("{}: Выходной контроль +{}ч (норматив), +{}сек (факт)", 
                transactionId, norm, work);
        }
    }

    logger.info("Итог для {}: норматив={}ч, факт={}сек", 
        employeeName, totalNormTime, totalWorkTime);
    return new int[]{totalNormTime, (int) totalWorkTime};
}



    private long parseWorkTime(String workTime) {
        if (workTime == null || "Нет данных".equalsIgnoreCase(workTime) || 
        "Контроль руководителя".equalsIgnoreCase(workTime)){
            return 0;
        }
        try {
            String[] parts = workTime.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            return hours * 3600 + minutes * 60 + seconds;
        } catch (Exception e) {
            logger.error("Error parsing work time: " + workTime, e);
            return 0;
        }
    }

        private String formatSecondsToTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    private int parseNormTime(String normTime) {
        try {
            return Integer.parseInt(normTime);
        } catch (NumberFormatException e) {
            logger.error("Error parsing norm time: " + normTime, e);
            return 0; // Return 0 if parsing fails
        }
    }

    private boolean isValidStopTime(String stopTime, YearMonth yearMonth) {
        logger.trace("Entering isValidStopTime() with stopTime: {}, yearMonth: {}", stopTime, yearMonth);
        long startTime = System.currentTimeMillis();

        if (stopTime == null || stopTime.isEmpty()) {
            logger.trace("Exiting isValidStopTime() - stopTime is null or empty");
            return false;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime stopDateTime = LocalDateTime.parse(stopTime, formatter);
            YearMonth stopYearMonth = YearMonth.from(stopDateTime);
            boolean result = stopYearMonth.equals(yearMonth);

            long endTime = System.currentTimeMillis();
            logger.trace("Exiting isValidStopTime() after {} ms. Result: {}", (endTime - startTime), result);
            return result;
        } catch (DateTimeParseException e) {
            // Обработка ошибки парсинга даты
            logger.error("Ошибка при парсинге даты: " + stopTime, e);
            return false;
        }
    }

    private Integer getHoursForYearMonth(YearMonth yearMonth) {
        logger.debug("Entering getHoursForYearMonth() with yearMonth: {}", yearMonth);
        long startTime = System.currentTimeMillis();

        Optional<first.sigmaback.entity.Time> timeOptional = timeRepository.findByMounth(yearMonth.atDay(1));

        if (timeOptional.isPresent()) {
            Integer hours = timeOptional.get().getHoursMounth();
            logger.debug("Found hours: {} for yearMonth: {}", hours, yearMonth);
            long endTime = System.currentTimeMillis();
            logger.debug("Exiting getHoursForYearMonth() after {} ms", (endTime - startTime));
            return hours;
        } else {
            logger.warn("No hours found for yearMonth: {}", yearMonth);
            long endTime = System.currentTimeMillis();
            logger.debug("Exiting getHoursForYearMonth() after {} ms", (endTime - startTime));
            return 0;
        }
    }
}