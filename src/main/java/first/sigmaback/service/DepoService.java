package first.sigmaback.service;

import first.sigmaback.dto.AnalisDTO;
import first.sigmaback.dto.DepoDto;
import first.sigmaback.dto.DepoFullDto;
import first.sigmaback.dto.DepoHeaderDto;
import first.sigmaback.dto.MonthlyTransactionCountDto;
import first.sigmaback.dto.EmployeesDto; // Импортируем EmployeesDto

import first.sigmaback.entity.DataCache;
import first.sigmaback.repository.DataCacheRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference; // Нужен для List<EmployeesDto>
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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

    @Autowired
    public DepoService(AnalisService analisService, MonthService monthService, DataCacheRepository dataCacheRepository, ObjectMapper objectMapper) {
        this.analisService = analisService;
        this.monthService = monthService;
        this.dataCacheRepository = dataCacheRepository;
        this.objectMapper = objectMapper;
    }

    public DepoFullDto getTransactionsInWork() {
        // --- Старый функционал ---

        // 1. Получаем все транзакции из AnalisService
        List<AnalisDTO> allTransactions = analisService.getAllTransactions().getTransactions();

        // 2. Фильтруем транзакции, оставляя только те, у которых статус "В работе"
        List<AnalisDTO> transactionsInWork = allTransactions.stream()
                .filter(transaction -> "В работе".equals(transaction.getStatus())) // Убедитесь, что статус "В работе" верен
                .collect(Collectors.toList());

        // 3. Преобразуем AnalisDTO в DepoDto и заполняем нужные поля
        List<DepoDto> depoTransactions = transactionsInWork.stream()
                .map(this::convertToDepoDto)
                .collect(Collectors.toList());

        // 4. Рассчитываем planPppSum
        Integer planPppSum = transactionsInWork.stream()
                .map(AnalisDTO::getPlanPpp)
                .filter(Objects::nonNull) // Фильтруем null значения
                .reduce(0, Integer::sum);

        // 5. Суммируем totalOperationsWorkTime в секундах и преобразуем в целые часы
        long totalSecondsOperations = transactionsInWork.stream()
                .map(AnalisDTO::getTotalOperationsWorkTime)
                .filter(Objects::nonNull)
                .mapToLong(this::parseTimeToSeconds)
                .sum();
        String totalOperationsWorkTimeSum = String.valueOf(totalSecondsOperations / 3600); // Целые часы

        // 6. Считаем totalTimeBetweenOperations в секундах и переводим в часы
        long totalTimeBetweenOperationsSeconds = transactionsInWork.stream()
                .map(AnalisDTO::getTotalTimeBetweenOperations)
                .filter(Objects::nonNull)
                .mapToLong(this::parseTimeToSeconds)
                .sum();
        int totalTimeBetweenOperationsHours = (int) (totalTimeBetweenOperationsSeconds / 3600);

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
    
    // 1. Расчёт процента выполнения (факт / план * 100)
    double percentage = ((double) totalOperationsWorkTimeHours / planPppSum) * 100;
    
    // 2. Вычитаем 100% и обрабатываем результат
    double deviationPercentage = percentage - 100;
    double finalPercentage = Math.max(deviationPercentage, 0); // Если < 0 → 0
    
    // 3. Форматируем (замена точки на запятую)
    planPppDiffPercentage = String.format("%.2f", finalPercentage).replace('.', ',');
    
} else if (planPppSum == null || planPppSum == 0) {
    planPppDiffPercentage = "Нет данных";
    planPppDiff = 0;
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

        // 7.2. Считаем количество "да" операций
        int yesOperationsCount = 0;
        for (DepoDto depo : depoTransactions) {
            if ("да".equals(depo.getVhodControlTimeExceeded())) yesOperationsCount++;
            if ("да".equals(depo.getElectricTimeExceeded())) yesOperationsCount++;
            if ("да".equals(depo.getMechanicTimeExceeded())) yesOperationsCount++;
            if ("да".equals(depo.getElectronTimeExceeded())) yesOperationsCount++;
            if ("да".equals(depo.getTechTimeExceeded())) yesOperationsCount++;
            if ("да".equals(depo.getVihodControlTimeExceeded())) yesOperationsCount++;
            if ("да".equals(depo.getTransportTimeExceeded())) yesOperationsCount++;
        }

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
long totalTimeAllSeconds = transactionsInWork.stream()
            .map(AnalisDTO::getTotalTimeAll)
            .filter(Objects::nonNull)
            .mapToLong(this::parseTimeToSeconds) // Используем существующий метод
            .sum();
// 7.5. Суммируем problemHours (если это Integer)
double totalProblemHours = transactionsInWork.stream()
        .map(AnalisDTO::getTotalProblemHours)
        .filter(Objects::nonNull) 
        .mapToDouble(Double::doubleValue)
        .sum();
    int totalTimeAllHours = (int) (totalTimeAllSeconds / 3600);
        // --- Новый функционал ---

        // Получаем данные за текущий месяц из кэша "employees_YYYY-MM"
        int[] monthlyWorkSummary = getMonthlyWorkSummary();
        int totalHoursMounth = monthlyWorkSummary[0];
        int totalWorkTimeHoursFromEmployees = monthlyWorkSummary[1];

        // 8. Создаем DepoHeaderDto и заполняем его данными (теперь 16 параметров)
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
                totalHoursMounth, // Новое значение
                totalWorkTimeHoursFromEmployees,
                totalTimeAllHours,
                totalProblemHours
        );

        // 9. Получаем monthlyTransactionCounts через MonthService
        List<MonthlyTransactionCountDto> monthlyTransactionCounts = monthService.getMonthlyTransactionCounts();

        // 10. Создаем DepoFullDto и устанавливаем в него header, транзакции и monthlyTransactionCounts
        DepoFullDto depoFullDto = new DepoFullDto();
        depoFullDto.setHeader(header);
        depoFullDto.setTransactions(depoTransactions);
        depoFullDto.setMonths(monthlyTransactionCounts);

        return depoFullDto;
    }
    private int[] getMonthlyWorkSummary() {
        logger.debug("Entering getMonthlyWorkSummary()");
        long startTime = System.currentTimeMillis();

        YearMonth currentMonth = YearMonth.now();
        String json = getMonthlyJson(currentMonth);

        // Десериализуем JSON в List<EmployeesDto>
        List<EmployeesDto> employeesData = deserializeEmployeesJson(json);

        if (employeesData == null || employeesData.isEmpty()) {
            logger.warn("Employees data is null or empty for cache: employees_{}", currentMonth);
            return new int[]{0, 0}; // Возвращаем 0, 0 в случае ошибки или отсутствия данных
        }

        int totalHoursMounth = 0;
        long totalWorkTimeSeconds = 0;

        for (EmployeesDto employee : employeesData) {
            // Проверяем, что totalWorkTime не null и не равен "00:00:00"
            if (employee.getTotalWorkTime() != null && !employee.getTotalWorkTime().equals("00:00:00")) {

                // Суммируем hoursMounth (уже Integer)
                if (employee.getHoursMounth() != null) {
                    totalHoursMounth += employee.getHoursMounth();
                }

                // Извлекаем totalWorkTime (String "HH:mm:ss") и конвертируем в секунды
                totalWorkTimeSeconds += parseTimeToSeconds(employee.getTotalWorkTime());
            }
        }

        // Конвертируем totalWorkTimeSeconds в часы
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

    /**
     * Вспомогательный метод для десериализации JSON в List<EmployeesDto>.
     * @param cachedJson JSON строка.
     * @return Список EmployeesDto или null в случае ошибки.
     */
    private List<EmployeesDto> deserializeEmployeesJson(String cachedJson) {
        if (cachedJson == null) {
            logger.warn("Cached JSON is null");
            return null;
        }

        try {
            // Используем TypeReference для десериализации списка
            TypeReference<List<EmployeesDto>> typeRef = new TypeReference<List<EmployeesDto>>() {};
            return objectMapper.readValue(cachedJson, typeRef);
        } catch (Exception e) {
            logger.error("Error while converting JSON to List<EmployeesDto>", e);
            return null;
        }
    }
    
    // --- Вспомогательные методы (оставшиеся без изменений) ---

    // Преобразуем AnalisDTO в DepoDto
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
        return "да";  // Добавлено: считаем как "да" для контроля руководителя
    }

    try {
        double percentage = Double.parseDouble(exceededTime.replace("%", "").replace(",", "."));
        if (percentage >= 100.0) return "да";
        else return "нет";
    } catch (NumberFormatException e) {
        logger.error("Ошибка при преобразовании строки в число: {}", exceededTime, e);
        return "Ошибка";
    }
}

    // Метод для создания DepoHeaderDto (сигнатура уже обновлена)
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
            double totalProblemHours
    ) {
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
        return header;
    }

    // Метод для преобразования времени в секунды
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