package first.sigmaback.service;

import first.sigmaback.dto.AnalisDTO;
import first.sigmaback.dto.AnalisFullDTO;
import first.sigmaback.dto.AnalisHeaderDTO;
import first.sigmaback.dto.DepoDto;
import first.sigmaback.dto.DepoFullDto;
import first.sigmaback.dto.DepoHeaderDto;
import first.sigmaback.dto.MonthlyTransactionCountDto;
import first.sigmaback.dto.EmployeesDto;

import first.sigmaback.entity.DataCache;
import first.sigmaback.repository.DataCacheRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DepoService {

    private static final Logger logger = LoggerFactory.getLogger(DepoService.class);

    private final AnalisService analisService;
    private final MonthService monthService;
    private final DataCacheRepository dataCacheRepository;
    private final ObjectMapper objectMapper;
    // Убираем AnalisHeaderService из зависимостей, так как будем получать данные через AnalisService
    // private final AnalisHeaderService analisHeaderService;

    @Autowired
    public DepoService(AnalisService analisService, 
                       MonthService monthService, 
                       DataCacheRepository dataCacheRepository, 
                       ObjectMapper objectMapper) {
        this.analisService = analisService;
        this.monthService = monthService;
        this.dataCacheRepository = dataCacheRepository;
        this.objectMapper = objectMapper;
        // this.analisHeaderService = analisHeaderService; // Убираем
    }

    public DepoFullDto getTransactionsInWork() {
        logger.info("Starting getTransactionsInWork()");
        long startTime = System.currentTimeMillis();

        // 1. Получаем все данные из AnalisService (включая header)
        AnalisFullDTO analisFullData = analisService.getAllTransactions();
        List<AnalisDTO> allTransactions = analisFullData.getTransactions();
        AnalisHeaderDTO analisHeader = analisFullData.getHeader(); // Получаем header из AnalisService
        
        logger.debug("Received {} transactions and header from AnalisService", allTransactions.size());

        // 2. Фильтруем транзакции, оставляя только те, у которых статус "В работе"
        List<AnalisDTO> transactionsInWork = allTransactions.stream()
                .filter(transaction -> "В работе".equals(transaction.getStatus()))
                .collect(Collectors.toList());
        
        logger.debug("Found {} transactions 'В работе'", transactionsInWork.size());

        // 3. Преобразуем AnalisDTO в DepoDto и заполняем нужные поля
        List<DepoDto> depoTransactions = transactionsInWork.stream()
                .map(this::convertToDepoDto)
                .collect(Collectors.toList());

        // 4. Рассчитываем planPppSum
        Integer planPppSum = transactionsInWork.stream()
                .map(AnalisDTO::getPlanPpp)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
        
        logger.debug("Plan PPP sum: {}", planPppSum);

        // 5. Суммируем totalOperationsWorkTime в секундах и преобразуем в целые часы
        long totalSecondsOperations = transactionsInWork.stream()
                .map(AnalisDTO::getTotalOperationsWorkTime)
                .filter(Objects::nonNull)
                .mapToLong(this::parseTimeToSeconds)
                .sum();
        String totalOperationsWorkTimeSum = String.valueOf(totalSecondsOperations / 3600);
        
        logger.debug("Total operations work time: {} seconds ({} hours)", totalSecondsOperations, totalOperationsWorkTimeSum);

        // 6. Считаем totalTimeBetweenOperations в секундах и переводим в часы
        long totalTimeBetweenOperationsSeconds = transactionsInWork.stream()
                .map(AnalisDTO::getTotalTimeBetweenOperations)
                .filter(Objects::nonNull)
                .mapToLong(this::parseTimeToSeconds)
                .sum();
        int totalTimeBetweenOperationsHours = (int) (totalTimeBetweenOperationsSeconds / 3600);
        
        logger.debug("Total time between operations: {} seconds ({} hours)", totalTimeBetweenOperationsSeconds, totalTimeBetweenOperationsHours);

        // 7. Рассчитываем разницу между planPppSum и totalOperationsWorkTime (в часах)
        int totalOperationsWorkTimeHours = 0;
        try {
            totalOperationsWorkTimeHours = Integer.parseInt(totalOperationsWorkTimeSum);
        } catch (NumberFormatException e) {
            logger.error("Ошибка при парсинге totalOperationsWorkTimeSum в int: {}", totalOperationsWorkTimeSum, e);
            totalOperationsWorkTimeHours = 0;
        }

        int planPppDiff = 0;
        String planPppDiffPercentage = "Нет данных";
        if (planPppSum != null && planPppSum != 0) {
            planPppDiff = Math.abs(planPppSum - totalOperationsWorkTimeHours);
            
            double percentage = ((double) totalOperationsWorkTimeHours / planPppSum) * 100;
            double deviationPercentage = percentage - 100;
            double finalPercentage = Math.max(deviationPercentage, 0);
            
            planPppDiffPercentage = String.format("%.2f", finalPercentage).replace('.', ',');
            
            logger.debug("Plan PPP difference calculation: {} hours, percentage: {}%", planPppDiff, planPppDiffPercentage);
        } else if (planPppSum == null || planPppSum == 0) {
            planPppDiffPercentage = "Нет данных";
            planPppDiff = 0;
            logger.debug("Plan PPP sum is null or zero, cannot calculate difference");
        }

        // 7.1. Считаем количество "нет" операций
        int noOperationsCount = 0;
        for (DepoDto depo : depoTransactions) {
            if ("нет".equals(depo.getVhodControlTimeExceeded())) noOperationsCount++;
            if ("нет".equals(depo.getElectricTimeExceeded())) noOperationsCount++;
            if ("нет".equals(depo.getMechanicTimeExceeded())) noOperationsCount++;
            if ("нет".equals(depo.getElectronTimeExceeded())) noOperationsCount++;
            if ("нет".equals(depo.getTechTimeExceeded())) noOperationsCount++;
            if ("нет".equals(depo.getVihodControlTimeExceeded())) noOperationsCount++;
            if ("нет".equals(depo.getTransportTimeExceeded())) noOperationsCount++;
        }
        
        logger.debug("No operations count: {}", noOperationsCount);

        // 7.2. Считаем количество "да" операций
        int yesOperationsCount = 0;
        for (DepoDto depo : depoTransactions) {
            if ("да".equals(depo.getVhodControlTimeExceeded()) || 
                "Контроль руководителя".equals(depo.getVhodControlTimeExceeded())) {
                yesOperationsCount++;
            }
            if ("да".equals(depo.getElectricTimeExceeded()) || 
                "Контроль руководителя".equals(depo.getElectricTimeExceeded())) {
                yesOperationsCount++;
            }
            if ("да".equals(depo.getMechanicTimeExceeded()) || 
                "Контроль руководителя".equals(depo.getMechanicTimeExceeded())) {
                yesOperationsCount++;
            }
            if ("да".equals(depo.getElectronTimeExceeded()) || 
                "Контроль руководителя".equals(depo.getElectronTimeExceeded())) {
                yesOperationsCount++;
            }
            if ("да".equals(depo.getTechTimeExceeded()) || 
                "Контроль руководителя".equals(depo.getTechTimeExceeded())) {
                yesOperationsCount++;
            }
            if ("да".equals(depo.getVihodControlTimeExceeded()) || 
                "Контроль руководителя".equals(depo.getVihodControlTimeExceeded())) {
                yesOperationsCount++;
            }
            if ("да".equals(depo.getTransportTimeExceeded()) || 
                "Контроль руководителя".equals(depo.getTransportTimeExceeded())) {
                yesOperationsCount++;
            }
        }
        
        logger.debug("Yes operations count: {}", yesOperationsCount);

        // 7.3. Считаем количество операций, не равных "Нет данных" для каждой операции
        int vhodControlExceededCount = 0, electricExceededCount = 0, mechanicExceededCount = 0,
            electronExceededCount = 0, techExceededCount = 0, vihodControlExceededCount = 0, transportExceededCount = 0;

        for (DepoDto depo : depoTransactions) {
            if (!"Нет данных".equals(depo.getVhodControlTimeExceeded())) vhodControlExceededCount++;
            if (!"Нет данных".equals(depo.getElectricTimeExceeded())) electricExceededCount++;
            if (!"Нет данных".equals(depo.getMechanicTimeExceeded())) mechanicExceededCount++;
            if (!"Нет данных".equals(depo.getElectronTimeExceeded())) electronExceededCount++;
            if (!"Нет данных".equals(depo.getTechTimeExceeded())) techExceededCount++;
            if (!"Нет данных".equals(depo.getVihodControlTimeExceeded())) vihodControlExceededCount++;
            if (!"Нет данных".equals(depo.getTransportTimeExceeded())) transportExceededCount++;
        }
        
        logger.debug("Exceeded counts - Vhod: {}, Electric: {}, Mechanic: {}, Electron: {}, Tech: {}, Vihod: {}, Transport: {}", 
                    vhodControlExceededCount, electricExceededCount, mechanicExceededCount, 
                    electronExceededCount, techExceededCount, vihodControlExceededCount, transportExceededCount);

        // 7.4. Суммируем problemHours
        double totalProblemHours = transactionsInWork.stream()
                .map(AnalisDTO::getTotalProblemHours)
                .filter(Objects::nonNull) 
                .mapToDouble(Double::doubleValue)
                .sum();
        
        logger.debug("Total problem hours: {}", totalProblemHours);
        
        int totalTimeAllHours = (int) (totalOperationsWorkTimeHours + totalTimeBetweenOperationsHours);
        logger.debug("Total time all hours: {}", totalTimeAllHours);

        // 8. Получаем данные за текущий месяц из кэша "employees_YYYY-MM"
        int[] monthlyWorkSummary = getMonthlyWorkSummary();
        int totalHoursMounth = monthlyWorkSummary[0];
        int totalWorkTimeHoursFromEmployees = monthlyWorkSummary[1];
        
        logger.debug("Monthly work summary - Total hours month: {}, Work time from employees: {}", 
                    totalHoursMounth, totalWorkTimeHoursFromEmployees);

        // 9. Рассчитываем общую статистику по персоналу из полученного header
        int totalEmployees = calculateTotalEmployees(analisHeader);
        int busyEmployees = calculateBusyEmployees(analisHeader);
        
        logger.debug("Personnel statistics - Total employees: {}, Busy employees: {}", totalEmployees, busyEmployees);
        
        // Создаем мапы для специальностей
        Map<String, Integer> employeesBySpecialization = createEmployeesBySpecializationMap(analisHeader);
        Map<String, Integer> busyEmployeesBySpecialization = createBusyEmployeesBySpecializationMap(analisHeader);
        
        logger.debug("Employees by specialization: {}", employeesBySpecialization);
        logger.debug("Busy employees by specialization: {}", busyEmployeesBySpecialization);

        // 10. Создаем DepoHeaderDto и заполняем его данными
        DepoHeaderDto header = createDepoHeaderDto(
                transactionsInWork.size(),
                planPppSum,
                totalOperationsWorkTimeSum,
                totalTimeBetweenOperationsHours,
                planPppDiff,
                planPppDiffPercentage,
                noOperationsCount,
                yesOperationsCount,
                vhodControlExceededCount,
                electricExceededCount,
                mechanicExceededCount,
                electronExceededCount,
                techExceededCount,
                vihodControlExceededCount,
                transportExceededCount,
                totalHoursMounth,
                totalWorkTimeHoursFromEmployees,
                totalTimeAllHours,
                totalProblemHours,
                totalEmployees,
                busyEmployees,
                employeesBySpecialization,
                busyEmployeesBySpecialization
        );

        // 11. Получаем monthlyTransactionCounts через MonthService
        List<MonthlyTransactionCountDto> monthlyTransactionCounts = monthService.getMonthlyTransactionCounts();
        logger.debug("Received {} monthly transaction counts", monthlyTransactionCounts.size());

        // 12. Создаем DepoFullDto и устанавливаем в него header, транзакции и monthlyTransactionCounts
        DepoFullDto depoFullDto = new DepoFullDto();
        depoFullDto.setHeader(header);
        depoFullDto.setTransactions(depoTransactions);
        depoFullDto.setMonths(monthlyTransactionCounts);

        long endTime = System.currentTimeMillis();
        logger.info("Finished getTransactionsInWork() in {} ms. Processed {} transactions 'В работе'", 
                   (endTime - startTime), transactionsInWork.size());
        
        return depoFullDto;
    }

    private int calculateTotalEmployees(AnalisHeaderDTO header) {
        if (header == null) {
            logger.warn("AnalisHeaderDTO is null, cannot calculate total employees");
            return 0;
        }
        
        int total = (header.getMechanicCount() != null ? header.getMechanicCount() : 0) +
                   (header.getEletronCount() != null ? header.getEletronCount() : 0) +
                   (header.getTechCount() != null ? header.getTechCount() : 0) +
                   (header.getElecCount() != null ? header.getElecCount() : 0) +
                   (header.getConplectCount() != null ? header.getConplectCount() : 0);
        
        logger.debug("Calculated total employees: {}", total);
        return total;
    }

    private int calculateBusyEmployees(AnalisHeaderDTO header) {
        if (header == null) {
            logger.warn("AnalisHeaderDTO is null, cannot calculate busy employees");
            return 0;
        }
        
        int busy = (header.getMechanicBusy() != null ? header.getMechanicBusy() : 0) +
                  (header.getEletronBusy() != null ? header.getEletronBusy() : 0) +
                  (header.getTechBusy() != null ? header.getTechBusy() : 0) +
                  (header.getElecBusy() != null ? header.getElecBusy() : 0) +
                  (header.getConplectBusy() != null ? header.getConplectBusy() : 0);
        
        logger.debug("Calculated busy employees: {}", busy);
        return busy;
    }

    private Map<String, Integer> createEmployeesBySpecializationMap(AnalisHeaderDTO header) {
        Map<String, Integer> map = new HashMap<>();
        if (header == null) {
            logger.warn("AnalisHeaderDTO is null, cannot create employees by specialization map");
            return map;
        }
        
        map.put("Механик", header.getMechanicCount() != null ? header.getMechanicCount() : 0);
        map.put("Электронщик", header.getEletronCount() != null ? header.getEletronCount() : 0);
        map.put("Технолог", header.getTechCount() != null ? header.getTechCount() : 0);
        map.put("Электрик", header.getElecCount() != null ? header.getElecCount() : 0);
        map.put("Комплектация", header.getConplectCount() != null ? header.getConplectCount() : 0);
        
        return map;
    }

    private Map<String, Integer> createBusyEmployeesBySpecializationMap(AnalisHeaderDTO header) {
        Map<String, Integer> map = new HashMap<>();
        if (header == null) {
            logger.warn("AnalisHeaderDTO is null, cannot create busy employees by specialization map");
            return map;
        }
        
        map.put("Механик", header.getMechanicBusy() != null ? header.getMechanicBusy() : 0);
        map.put("Электронщик", header.getEletronBusy() != null ? header.getEletronBusy() : 0);
        map.put("Технолог", header.getTechBusy() != null ? header.getTechBusy() : 0);
        map.put("Электрик", header.getElecBusy() != null ? header.getElecBusy() : 0);
        map.put("Комплектация", header.getConplectBusy() != null ? header.getConplectBusy() : 0);
        
        return map;
    }

    private DepoHeaderDto createDepoHeaderDto(
            long totalTransactionsInWork,
            Integer planPppSum,
            String totalOperationsWorkTimeSum,
            int totalTimeBetweenOperationsHours,
            int planPppDiff,
            String planPppDiffPercentage,
            int noOperationsCount,
            int yesOperationsCount,
            int vhodControlExceededCount,
            int electricExceededCount,
            int mechanicExceededCount,
            int electronExceededCount,
            int techExceededCount,
            int vihodControlExceededCount,
            int transportExceededCount,
            int totalHoursMounth,
            int totalWorkTimeHoursFromEmployees,
            int totalTimeAllHours,
            double totalProblemHours,
            int totalEmployees,
            int busyEmployees,
            Map<String, Integer> employeesBySpecialization,
            Map<String, Integer> busyEmployeesBySpecialization
    ) {
        logger.debug("Creating DepoHeaderDto with parameters:");
        logger.debug("  Total transactions in work: {}", totalTransactionsInWork);
        logger.debug("  Total employees: {}, Busy employees: {}", totalEmployees, busyEmployees);
        
        DepoHeaderDto header = new DepoHeaderDto();
        header.setTotalTransactionsInWork(totalTransactionsInWork);
        header.setPlanPppSum(planPppSum);
        header.setTotalOperationsWorkTimeSum(totalOperationsWorkTimeSum);
        header.setTotalTimeBetweenOperationsHours(totalTimeBetweenOperationsHours);
        header.setPlanPppDiff(planPppDiff);
        header.setPlanPppDiffPercentage(planPppDiffPercentage);
        header.setNoOperationsCount(noOperationsCount);
        header.setYesOperationsCount(yesOperationsCount);
        header.setVhodControlExceededCount(vhodControlExceededCount);
        header.setElectricExceededCount(electricExceededCount);
        header.setMechanicExceededCount(mechanicExceededCount);
        header.setElectronExceededCount(electronExceededCount);
        header.setTechExceededCount(techExceededCount);
        header.setVihodControlExceededCount(vihodControlExceededCount);
        header.setTransportExceededCount(transportExceededCount);
        header.setTotalHoursMounth(totalHoursMounth);
        header.setTotalWorkTimeHoursFromEmployees(totalWorkTimeHoursFromEmployees);
        header.setTotalTimeAllHours(totalTimeAllHours);
        header.setTotalProblemHours(totalProblemHours);
        
        header.setTotalEmployees(totalEmployees);
        header.setBusyEmployees(busyEmployees);
        header.setEmployeesBySpecialization(employeesBySpecialization);
        header.setBusyEmployeesBySpecialization(busyEmployeesBySpecialization);
        
        return header;
    }

    private DepoDto convertToDepoDto(AnalisDTO analisDTO) {
        DepoDto depoDto = new DepoDto();
        depoDto.setTransaction(analisDTO.getTransaction());
        depoDto.setVhodControlTimeExceeded(processExceededTime(analisDTO.getVhodControlTimeExceeded()));
        depoDto.setElectricTimeExceeded(processExceededTime(analisDTO.getElectricTimeExceeded()));
        depoDto.setMechanicTimeExceeded(processExceededTime(analisDTO.getMechanicTimeExceeded()));
        depoDto.setElectronTimeExceeded(processExceededTime(analisDTO.getElectronTimeExceeded()));
        depoDto.setTechTimeExceeded(processExceededTime(analisDTO.getTechTimeExceeded()));
        depoDto.setVihodControlTimeExceeded(processExceededTime(analisDTO.getVihodControlTimeExceeded()));
        depoDto.setTransportTimeExceeded(processExceededTime(analisDTO.getTransportTimeExceeded()));
        return depoDto;
    }

    private String processExceededTime(String exceededTime) {
        if ("Нет данных".equals(exceededTime)) {
            return "Нет данных";
        }
        if ("Контроль руководителя".equals(exceededTime)) {
            return "Контроль руководителя";
        }

        try {
            double percentage = Double.parseDouble(exceededTime.replace("%", "").replace(",", "."));
            if (percentage <= 100.0) return "да";
            else return "нет";
        } catch (NumberFormatException e) {
            logger.error("Ошибка при преобразовании строки в число: {}", exceededTime, e);
            return "Ошибка";
        }
    }

    private int[] getMonthlyWorkSummary() {
        logger.debug("Entering getMonthlyWorkSummary()");
        long startTime = System.currentTimeMillis();

        YearMonth currentMonth = YearMonth.now();
        String json = getMonthlyJson(currentMonth);

        List<EmployeesDto> employeesData = deserializeEmployeesJson(json);

        if (employeesData == null || employeesData.isEmpty()) {
            logger.warn("Employees data is null or empty for cache: employees_{}", currentMonth);
            return new int[]{0, 0};
        }

        int totalHoursMounth = 0;
        long totalWorkTimeSeconds = 0;

        for (EmployeesDto employee : employeesData) {
            if (employee.getTotalWorkTime() != null && !employee.getTotalWorkTime().equals("00:00:00")) {
                if (employee.getHoursMounth() != null) {
                    totalHoursMounth += employee.getHoursMounth();
                }
                totalWorkTimeSeconds += parseTimeToSeconds(employee.getTotalWorkTime());
            }
        }

        int totalWorkTimeHours = (int) (totalWorkTimeSeconds / 3600);

        long endTime = System.currentTimeMillis();
        logger.debug("Exiting getMonthlyWorkSummary() after {} ms. TotalHoursMounth: {}, TotalWorkTimeHours: {}",
                     (endTime - startTime), totalHoursMounth, totalWorkTimeHours);

        return new int[]{totalHoursMounth, totalWorkTimeHours};
    }

    private String getMonthlyJson(YearMonth yearMonth) {
        String cacheName = "employees_" + yearMonth.toString();
        Optional<DataCache> dataCacheOptional = dataCacheRepository.findByJsonName(cacheName);
        if (!dataCacheOptional.isPresent()) {
            logger.warn("DataCache with name '{}' not found.", cacheName);
            return null;
        }
        return dataCacheOptional.get().getJson();
    }

    private List<EmployeesDto> deserializeEmployeesJson(String cachedJson) {
        if (cachedJson == null) {
            logger.warn("Cached JSON is null");
            return null;
        }

        try {
            TypeReference<List<EmployeesDto>> typeRef = new TypeReference<List<EmployeesDto>>() {};
            return objectMapper.readValue(cachedJson, typeRef);
        } catch (Exception e) {
            logger.error("Error while converting JSON to List<EmployeesDto>", e);
            return null;
        }
    }

    private long parseTimeToSeconds(String time) {
        if (time == null || time.isEmpty()) return 0;

        String[] parts = time.split(":");
        if (parts.length != 3) {
            logger.error("Неверный формат времени: {}", time);
            return 0;
        }

        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            return hours * 3600L + minutes * 60 + seconds;
        } catch (NumberFormatException e) {
            logger.error("Ошибка при парсинге времени: {}", time, e);
            return 0;
        }
    }
}