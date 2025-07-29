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
    logger.debug("Entering getTransactionCountForEmployee() with employeeName: {} and yearMonth: {}", employeeName, yearMonth);
    long startTime = System.currentTimeMillis();
    int transactionCount = 0;
    int exceededTimeCount = 0;

    // 1. Get DataCache from DataCacheRepository
    Optional<DataCache> dataCacheOptional = dataCacheRepository.findByJsonName("Первый json");
    if (!dataCacheOptional.isPresent()) {
        logger.warn("DataCache with name 'Первый json' not found");
        return new int[]{0, 0};
    }

    DataCache dataCache = dataCacheOptional.get();
    String cachedJson = dataCache.getJson();

    if (cachedJson == null) {
        logger.warn("Cached JSON is null");
        return new int[]{0, 0};
    }

    // 2. Convert JSON to AnalisFullDTO
    AnalisFullDTO analisFullData;
    try {
        analisFullData = objectMapper.readValue(cachedJson, AnalisFullDTO.class);
    } catch (Exception e) {
        logger.error("Error while converting JSON to AnalisFullDTO", e);
        return new int[]{0, 0};
    }

    if (analisFullData == null || analisFullData.getTransactions() == null) {
        logger.warn("AnalisService returned null data");
        return new int[]{0, 0};
    }

    // 3. First filtering - by factDateStop
    List<AnalisDTO> filteredTransactions = analisFullData.getTransactions().stream()
            .filter(transaction -> {
                LocalDate factDateStop = transaction.getFactDateStop();
                return factDateStop != null && 
                       YearMonth.from(factDateStop).equals(yearMonth);
            })
            .collect(Collectors.toList());

    logger.debug("After factDateStop filtering, {} transactions remain", filteredTransactions.size());

    // 4. Second filtering - check stopTime for each operation
   for (AnalisDTO transaction : filteredTransactions) {
    // Vhod Control
    if (employeeName.equals(transaction.getVhodControlEmployee()) && 
        isValidStopTime(transaction.getVhodControlStopTime(), yearMonth)) {
        transactionCount++;
        String timeExceeded = transaction.getVhodControlTimeExceeded();
        if (!"Нет данных".equalsIgnoreCase(timeExceeded) && 
            !"Контроль руководителя".equalsIgnoreCase(timeExceeded)) {
            exceededTimeCount += isTimeExceeded(timeExceeded);
        }
    }
    
    // Podkluchenie
    if (employeeName.equals(transaction.getPodkluchenieEmployee()) && 
        isValidStopTime(transaction.getPodkluchenieStopTime(), yearMonth)) {
        transactionCount++;
        String timeExceeded = transaction.getElectricTimeExceeded();
        if (!"Нет данных".equalsIgnoreCase(timeExceeded) && 
            !"Контроль руководителя".equalsIgnoreCase(timeExceeded)) {
            exceededTimeCount += isTimeExceeded(timeExceeded);
        }
    }
    
    // Proverka Mehanikom
    if (employeeName.equals(transaction.getProverkaMehanikomEmployee()) && 
        isValidStopTime(transaction.getProverkaMehanikomStopTime(), yearMonth)) {
        transactionCount++;
        String timeExceeded = transaction.getMechanicTimeExceeded();
        if (!"Нет данных".equalsIgnoreCase(timeExceeded) && 
            !"Контроль руководителя".equalsIgnoreCase(timeExceeded)) {
            exceededTimeCount += isTimeExceeded(timeExceeded);
        }
    }
    
    // Proverka Electron
    if (employeeName.equals(transaction.getProverkaElectronEmployee()) && 
        isValidStopTime(transaction.getProverkaElectronStopTime(), yearMonth)) {
        transactionCount++;
        String timeExceeded = transaction.getElectronTimeExceeded();
        if (!"Нет данных".equalsIgnoreCase(timeExceeded) && 
            !"Контроль руководителя".equalsIgnoreCase(timeExceeded)) {
            exceededTimeCount += isTimeExceeded(timeExceeded);
        }
    }
    
    // Proverka Tehnologom
    if (employeeName.equals(transaction.getProverkaTehnologomEmployee()) && 
        isValidStopTime(transaction.getProverkaTehnologomStopTime(), yearMonth)) {
        transactionCount++;
        String timeExceeded = transaction.getTechTimeExceeded();
        if (!"Нет данных".equalsIgnoreCase(timeExceeded) && 
            !"Контроль руководителя".equalsIgnoreCase(timeExceeded)) {
            exceededTimeCount += isTimeExceeded(timeExceeded);
        }
    }
    
    // Transport Polozhenie
    if (employeeName.equals(transaction.getTransportPolozhenieEmployee()) && 
        isValidStopTime(transaction.getTransportPolozhenieStopTime(), yearMonth)) {
        transactionCount++;
        String timeExceeded = transaction.getTransportTimeExceeded();
        if (!"Нет данных".equalsIgnoreCase(timeExceeded) && 
            !"Контроль руководителя".equalsIgnoreCase(timeExceeded)) {
            exceededTimeCount += isTimeExceeded(timeExceeded);
        }
    }
    
    // Vihod Control
    if (employeeName.equals(transaction.getVihodControlEmployee()) && 
        isValidStopTime(transaction.getVihodControlStopTime(), yearMonth)) {
        transactionCount++;
        String timeExceeded = transaction.getVihodControlTimeExceeded();
        if (!"Нет данных".equalsIgnoreCase(timeExceeded) && 
            !"Контроль руководителя".equalsIgnoreCase(timeExceeded)) {
            exceededTimeCount += isTimeExceeded(timeExceeded);
        }
    }
}

    long endTime = System.currentTimeMillis();
    logger.debug("Exiting getTransactionCountForEmployee(). Total transactions: {}, exceeded: {}, time: {} ms",
            transactionCount, exceededTimeCount, (endTime - startTime));
    return new int[]{transactionCount, exceededTimeCount};
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

   private int[] getTotalNormTimeForEmployee(String employeeName, YearMonth yearMonth) {
    logger.debug("Entering getTotalNormTimeForEmployeeOptimized() for employee: {} and yearMonth: {}", 
        employeeName, yearMonth);
    long startTime = System.currentTimeMillis();
    int totalNormTime = 0;
    long totalWorkTime = 0;

    // 1. Получаем DataCache из репозитория
    Optional<DataCache> dataCacheOptional = dataCacheRepository.findByJsonName("Первый json");
    if (!dataCacheOptional.isPresent()) {
        logger.warn("DataCache with name 'Первый json' not found for employee: {}", employeeName);
        return new int[]{0, 0};
    }

    DataCache dataCache = dataCacheOptional.get();
    String cachedJson = dataCache.getJson();

    if (cachedJson == null) {
        logger.warn("Cached JSON is null for employee: {}", employeeName);
        return new int[]{0, 0};
    }

    // 2. Конвертируем JSON в AnalisFullDTO
    AnalisFullDTO analisFullData;
    try {
        analisFullData = objectMapper.readValue(cachedJson, AnalisFullDTO.class);
    } catch (Exception e) {
        logger.error("Error while converting JSON to AnalisFullDTO for employee: {}", employeeName, e);
        return new int[]{0, 0};
    }

    if (analisFullData == null || analisFullData.getTransactions() == null) {
        logger.warn("AnalisService returned null data for employee: {}", employeeName);
        return new int[]{0, 0};
    }

    // 3. Фильтруем транзакции
    List<AnalisDTO> filteredTransactions = analisFullData.getTransactions().stream()
            .filter(transaction -> {
                LocalDate factDateStop = transaction.getFactDateStop();
                return factDateStop != null && 
                       YearMonth.from(factDateStop).equals(yearMonth);
            })
            .collect(Collectors.toList());

    logger.debug("For employee: {} found {} transactions in month: {}", 
        employeeName, filteredTransactions.size(), yearMonth);

    // 4. Получаем заголовок (нормативы)
    AnalisHeaderDTO header;
    try {
        header = objectMapper.convertValue(analisFullData.getHeader(), AnalisHeaderDTO.class);
    } catch (Exception e) {
        logger.error("Error while converting header to AnalisHeaderDTO for employee: {}", employeeName, e);
        return new int[]{0, 0};
    }

    // 5. Считаем время только для отфильтрованных транзакций
    for (AnalisDTO transaction : filteredTransactions) {
        String transactionId = transaction.getTransaction(); // предполагаем, что у транзакции есть ID
        logger.debug("Processing transaction {} for employee: {}", transactionId, employeeName);

        // Vhod Control
        if (employeeName.equals(transaction.getVhodControlEmployee())) {
            int norm = parseNormTime(header.getVhodNorm());
            long work = parseWorkTime(transaction.getVhodControlWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.info("Employee {}: Vhod Control - added norm: {} sec ({}), work: {} sec ({}), transaction{}", 
                employeeName, norm, formatSecondsToTime(norm), 
                work, formatSecondsToTime((int)work),transactionId);
        }
        
        // Podkluchenie
        if (employeeName.equals(transaction.getPodkluchenieEmployee())) {
            int norm = parseNormTime(header.getPodklyuchenieNorm());
            long work = parseWorkTime(transaction.getPodkluchenieWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.info("Employee {}: Podkluchenie - added norm: {} sec ({}), work: {} sec ({})", 
                employeeName, norm, formatSecondsToTime(norm), 
                work, formatSecondsToTime((int)work));
        }
        
        // Proverka Mehanikom
        if (employeeName.equals(transaction.getProverkaMehanikomEmployee())) {
            int norm = parseNormTime(header.getMechOperationNorm());
            long work = parseWorkTime(transaction.getProverkaMehanikomWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.info("Employee {}: Proverka Mehanikom - added norm: {} sec ({}), work: {} sec ({})", 
                employeeName, norm, formatSecondsToTime(norm), 
                work, formatSecondsToTime((int)work));
        }
        
        // Proverka Electron
        if (employeeName.equals(transaction.getProverkaElectronEmployee())) {
            int norm = parseNormTime(header.getElectronOperationNorm());
            long work = parseWorkTime(transaction.getProverkaElectronWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.info("Employee {}: Proverka Electron - added norm: {} sec ({}), work: {} sec ({})", 
                employeeName, norm, formatSecondsToTime(norm), 
                work, formatSecondsToTime((int)work));
        }
        
        // Proverka Tehnologom
        if (employeeName.equals(transaction.getProverkaTehnologomEmployee())) {
            int norm = parseNormTime(header.getTechOperationNorm());
            long work = parseWorkTime(transaction.getProverkaTehnologomWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.info("Employee {}: Proverka Tehnologom - added norm: {} sec ({}), work: {} sec ({})", 
                employeeName, norm, formatSecondsToTime(norm), 
                work, formatSecondsToTime((int)work));
        }
        
        // Vihod Control
        if (employeeName.equals(transaction.getVihodControlEmployee())) {
            int norm = parseNormTime(header.getVihodNorm());
            long work = parseWorkTime(transaction.getVihodControlWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.info("Employee {}: Vihod Control - added norm: {} sec ({}), work: {} sec ({}), transaction {}", 
                employeeName, norm, formatSecondsToTime(norm), 
                work, formatSecondsToTime((int)work), transactionId);
        }
        
        // Transport Polozhenie
        if (employeeName.equals(transaction.getTransportPolozhenieEmployee())) {
            int norm = parseNormTime(header.getTransportNorm());
            long work = parseWorkTime(transaction.getTransportPolozhenieWorkTime());
            totalNormTime += norm;
            totalWorkTime += work;
            logger.info("Employee {}: Transport Polozhenie - added norm: {} sec ({}), work: {} sec ({})", 
                employeeName, norm, formatSecondsToTime(norm), 
                work, formatSecondsToTime((int)work));
        }
    }

    long endTime = System.currentTimeMillis();
    logger.info("Employee {}: Calculated total norm: {} sec ({}), work: {} sec ({}), processing time: {} ms",
        employeeName, 
        totalNormTime, formatSecondsToTime(totalNormTime),
        totalWorkTime, formatSecondsToTime((int)totalWorkTime),
        (endTime - startTime));
    
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