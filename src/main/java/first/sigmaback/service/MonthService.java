package first.sigmaback.service;

import first.sigmaback.repository.DataCacheRepository;
import first.sigmaback.dto.AnalisDTO;
import first.sigmaback.dto.MonthlyTransactionCountDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import first.sigmaback.entity.DataCache;
import first.sigmaback.dto.AnalisFullDTO;  //  Импортируем AnalisFullDTO
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class MonthService {

    private static final Logger logger = LoggerFactory.getLogger(MonthService.class);

    private final DataCacheRepository dataCacheRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public MonthService(DataCacheRepository dataCacheRepository, ObjectMapper objectMapper) {
        this.dataCacheRepository = dataCacheRepository;
        this.objectMapper = objectMapper;
    }

    public List<MonthlyTransactionCountDto> getMonthlyTransactionCounts() {
        logger.debug("Entering getMonthlyTransactionCounts()");
        long startTime = System.currentTimeMillis();

        // 1. Get DataCache from DataCacheRepository
        DataCache dataCache = getDataCache();
        if (dataCache == null) {
            logger.warn("DataCache not found or JSON is null");
            return new ArrayList<>();
        }

        // 2. Deserialize JSON to AnalisFullDTO
        AnalisFullDTO analisFullData = deserializeJson(dataCache.getJson());
        if (analisFullData == null || analisFullData.getTransactions() == null) {
            logger.warn("AnalisFullDTO or transactions are null");
            return new ArrayList<>();
        }

        List<AnalisDTO> allTransactions = analisFullData.getTransactions();

        // 3. Определяем начальный и конечный месяцы
        YearMonth startMonth = YearMonth.of(2023, 1);
        YearMonth endMonth = YearMonth.now();

        // 4. Создаем список всех месяцев в диапазоне
        List<YearMonth> allMonths = new ArrayList<>();
        YearMonth currentMonth = startMonth;
        while (!currentMonth.isAfter(endMonth)) {
            allMonths.add(currentMonth);
            currentMonth = currentMonth.plusMonths(1);
        }

        // 5. Формируем список MonthlyTransactionCountDto
        List<MonthlyTransactionCountDto> monthlyTransactionCounts = new ArrayList<>();

        for (YearMonth month : allMonths) {
            MonthlyTransactionCountDto dto = new MonthlyTransactionCountDto();
            dto.setMonth(month);
            long count = 0;
            for (AnalisDTO transaction : allTransactions) {
                if ("Закрыта".equals(transaction.getStatus()) && transaction.getFactDateStop() != null) {
                    try {
                        YearMonth factMonth = YearMonth.from((LocalDate) transaction.getFactDateStop());  // Преобразуем LocalDate в YearMonth
                        if (factMonth.equals(month)) {
                            count++;
                        }
                    } catch (Exception e) {
                        logger.error("Error while processing factDateStop: {}", transaction.getFactDateStop(), e); // Изменили сообщение об ошибке
                    }
                }
            }
            dto.setTransactionCount(count);
            monthlyTransactionCounts.add(dto);
        }

        long endTime = System.currentTimeMillis();
        logger.debug("Exiting getMonthlyTransactionCounts() after {} ms", (endTime - startTime));
        return monthlyTransactionCounts;
    }

    // Helper methods
    private DataCache getDataCache() {
        Optional<DataCache> dataCacheOptional = dataCacheRepository.findByJsonName("Первый json");
        if (!dataCacheOptional.isPresent()) {
            logger.warn("DataCache with name 'Первый json' not found");
            return null;
        }
        return dataCacheOptional.get();
    }

    private AnalisFullDTO deserializeJson(String cachedJson) {
        if (cachedJson == null) {
            logger.warn("Cached JSON is null");
            return null;
        }

        try {
            return objectMapper.readValue(cachedJson, AnalisFullDTO.class);
        } catch (Exception e) {
            logger.error("Error while converting JSON to AnalisFullDTO", e);
            return null;
        }
    }
}