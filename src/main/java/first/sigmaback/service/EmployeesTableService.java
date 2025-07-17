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
            double percentage = ((double) normAndWorkTime[1]/ hoursMounth*3600 ) * 100;
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
        List<AnalisDTO> analisData = analisFullData.getTransactions();

        if (analisData == null) {
            logger.warn("AnalisService returned null data");
            return new int[]{0, 0};
        }
        logger.debug("AnalisService returned {} transactions", analisData.size());

        // 3. Iterate through transactions
        for (AnalisDTO transaction : analisData) {
            //Проверяем каждую роль и соответствующее превышение времени
            if (employeeName.equals(transaction.getVhodControlEmployee()) && isValidStopTime(transaction.getVhodControlStopTime(), yearMonth)) {
                transactionCount++;
                if (!"Нет данных".equalsIgnoreCase(transaction.getVhodControlTimeExceeded())) {
                    exceededTimeCount += isTimeExceeded(transaction.getVhodControlTimeExceeded());
                }
            }
            if (employeeName.equals(transaction.getPodkluchenieEmployee()) && isValidStopTime(transaction.getPodkluchenieStopTime(), yearMonth)) {
                transactionCount++;
                if (!"Нет данных".equalsIgnoreCase(transaction.getElectricTimeExceeded())) {
                    exceededTimeCount += isTimeExceeded(transaction.getElectricTimeExceeded());
                }
            }
            if (employeeName.equals(transaction.getProverkaMehanikomEmployee()) && isValidStopTime(transaction.getProverkaMehanikomStopTime(), yearMonth)) {
                transactionCount++;
                if (!"Нет данных".equalsIgnoreCase(transaction.getMechanicTimeExceeded())) {
                    exceededTimeCount += isTimeExceeded(transaction.getMechanicTimeExceeded());
                }
            }
            if (employeeName.equals(transaction.getProverkaElectronEmployee()) && isValidStopTime(transaction.getProverkaElectronStopTime(), yearMonth)) {
                transactionCount++;
                if (!"Нет данных".equalsIgnoreCase(transaction.getElectronTimeExceeded())) {
                    exceededTimeCount += isTimeExceeded(transaction.getElectronTimeExceeded());
                }
            }
            if (employeeName.equals(transaction.getProverkaTehnologomEmployee()) && isValidStopTime(transaction.getProverkaTehnologomStopTime(), yearMonth)) {
                transactionCount++;
                if (!"Нет данных".equalsIgnoreCase(transaction.getTechTimeExceeded())) {
                    exceededTimeCount += isTimeExceeded(transaction.getTechTimeExceeded());
                }
            }
            if (employeeName.equals(transaction.getTransportPolozhenieEmployee()) && isValidStopTime(transaction.getTransportPolozhenieStopTime(), yearMonth)) {
                transactionCount++;
                if (!"Нет данных".equalsIgnoreCase(transaction.getTransportTimeExceeded())) {
                    exceededTimeCount += isTimeExceeded(transaction.getTransportTimeExceeded());
                }
            }
            if (employeeName.equals(transaction.getVihodControlEmployee()) && isValidStopTime(transaction.getVihodControlStopTime(), yearMonth)) {
                transactionCount++;
                if (!"Нет данных".equalsIgnoreCase(transaction.getVihodControlTimeExceeded())) {
                    exceededTimeCount += isTimeExceeded(transaction.getVihodControlTimeExceeded());
                }
            }
        }

        long endTime = System.currentTimeMillis();
        logger.debug("Exiting getTransactionCountForEmployee() after {} ms. Transaction count: {}, exceededTimeCount: {}", (endTime - startTime), transactionCount, exceededTimeCount);
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
        logger.debug("Entering getTotalNormTimeForEmployee() with employeeName: {} and yearMonth: {}", employeeName, yearMonth);
        long startTime = System.currentTimeMillis();
        int totalNormTime = 0;
        long totalWorkTime = 0;

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
        List<AnalisDTO> analisData = analisFullData.getTransactions();

        if (analisData == null) {
            logger.warn("AnalisService returned null data");
            return new int[]{0, 0};
        }
        logger.debug("AnalisService returned {} transactions", analisData.size());

        // 3. Get header and convert to AnalisHeaderDTO
        AnalisHeaderDTO header;
        try {
            header = objectMapper.convertValue(analisFullData.getHeader(), AnalisHeaderDTO.class);
        } catch (Exception e) {
            logger.error("Error while converting header to AnalisHeaderDTO", e);
            return new int[]{0, 0};
        }

        // 4. Iterate through transactions and calculate total norm time and work time
        for (AnalisDTO transaction : analisData) {
            if (employeeName.equals(transaction.getVhodControlEmployee()) && isValidStopTime(transaction.getVhodControlStopTime(), yearMonth)) {
                totalNormTime += parseNormTime(header.getVhodNorm());
                totalWorkTime += parseWorkTime(transaction.getVhodControlWorkTime());
            }
            if (employeeName.equals(transaction.getPodkluchenieEmployee()) && isValidStopTime(transaction.getPodkluchenieStopTime(), yearMonth)) {
                totalNormTime += parseNormTime(header.getPodklyuchenieNorm());
                totalWorkTime += parseWorkTime(transaction.getPodkluchenieWorkTime());
            }
            if (employeeName.equals(transaction.getProverkaMehanikomEmployee()) && isValidStopTime(transaction.getProverkaMehanikomStopTime(), yearMonth)) {
                totalNormTime += parseNormTime(header.getMechOperationNorm());
                totalWorkTime += parseWorkTime(transaction.getProverkaMehanikomWorkTime());
            }
            if (employeeName.equals(transaction.getProverkaElectronEmployee()) && isValidStopTime(transaction.getProverkaElectronStopTime(), yearMonth)) {
                totalNormTime += parseNormTime(header.getElectronOperationNorm());
                totalWorkTime += parseWorkTime(transaction.getProverkaElectronWorkTime());
            }
            if (employeeName.equals(transaction.getProverkaTehnologomEmployee()) && isValidStopTime(transaction.getProverkaTehnologomStopTime(), yearMonth)) {
                totalNormTime += parseNormTime(header.getTechOperationNorm());
                totalWorkTime += parseWorkTime(transaction.getProverkaTehnologomWorkTime());
            }
            if (employeeName.equals(transaction.getVihodControlEmployee()) && isValidStopTime(transaction.getVihodControlStopTime(), yearMonth)) {
                totalNormTime += parseNormTime(header.getVihodNorm());
                totalWorkTime += parseWorkTime(transaction.getVihodControlWorkTime());
            }
            if (employeeName.equals(transaction.getTransportPolozhenieEmployee()) && isValidStopTime(transaction.getTransportPolozhenieStopTime(), yearMonth)) {
                totalNormTime += parseNormTime(header.getTransportNorm());
                totalWorkTime += parseWorkTime(transaction.getTransportPolozhenieWorkTime());
            }
        }

        long endTime = System.currentTimeMillis();
        logger.debug("Exiting getTotalNormTimeForEmployee() after {} ms. Total norm time: {}, total work time: {}", (endTime - startTime), totalNormTime, totalWorkTime);
        return new int[]{totalNormTime, (int)totalWorkTime};
    }

    private long parseWorkTime(String workTime) {
        if (workTime == null || "Нет данных".equalsIgnoreCase(workTime)) {
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