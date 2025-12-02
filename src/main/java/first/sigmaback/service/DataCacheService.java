package first.sigmaback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import first.sigmaback.dto.AnalisFullDTO;
import first.sigmaback.dto.DepoFullDto;
import first.sigmaback.dto.EmployeesDto;
import first.sigmaback.entity.DataCache;
import first.sigmaback.repository.DataCacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DataCacheService {

    private final DataCacheRepository dataCacheRepository;
    private final ObjectMapper objectMapper;
    private final AnalisService analisService;
    private final DepoService depoService;
    private final EmployeesTableService employeesTableService;
    
    // ДОБАВЛЯЕМ СИНХРОНИЗАЦИЮ
    private final ReentrantLock cacheLock = new ReentrantLock();
    private volatile boolean isCacheLoading = false;

    @Autowired
    public DataCacheService(DataCacheRepository dataCacheRepository, ObjectMapper objectMapper, AnalisService analisService, DepoService depoService, EmployeesTableService employeesTableService) {
        this.dataCacheRepository = dataCacheRepository;
        this.objectMapper = objectMapper;
        this.analisService = analisService;
        this.depoService = depoService;
        this.employeesTableService = employeesTableService;
        loadCache(); // Загружаем данные при создании сервиса
    }

    // Получение данных (запрос)
    public String getCachedData(String name) {
        Optional<DataCache> cacheEntry = dataCacheRepository.findByJsonName(name);
        return cacheEntry.map(DataCache::getJson).orElse(null);
    }

    // Загрузка кэша при запуске приложения и по расписанию
    @Scheduled(fixedRate = 3000000) // Запускать каждые 30 минут (1800000 миллисекунд)
    @Transactional
    public void loadCache() {
        // ПРОВЕРКА ЧТО КЭШ УЖЕ ЗАГРУЖАЕТСЯ
        if (isCacheLoading) {
            System.out.println("Кэш уже загружается, пропускаем вызов...");
            return;
        }
        
        cacheLock.lock();
        try {
            isCacheLoading = true;
            System.out.println("Начало загрузки кэша...");
            
            cacheAnalisData();
            generateAndCacheEmployeesData();
            updateCurrentMonthEmployeesCache(); 
            cacheDepoData();
            
            System.out.println("Загрузка кэша завершена успешно.");
        } finally {
            isCacheLoading = false;
            cacheLock.unlock();
        }
    }

    private void cacheAnalisData() {
        String name = "Первый json";
        try {
            // 1. Получаем данные
            AnalisFullDTO data = fetchDataFromAnalisService();

            // 2. Сериализуем данные в JSON
            String jsonData = objectMapper.writeValueAsString(data);

            // 3. Получаем текущее время
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            String formattedTime = now.format(formatter);

            // 4. Сохраняем данные в базу данных
            Optional<DataCache> existingEntry = dataCacheRepository.findByJsonName(name);
            DataCache cacheEntry;
            if (existingEntry.isPresent()) {
                cacheEntry = existingEntry.get();
            } else {
                cacheEntry = new DataCache();
                cacheEntry.setJsonName(name);
            }

            cacheEntry.setJson(jsonData);
            cacheEntry.setJsonTime(formattedTime);
            dataCacheRepository.save(cacheEntry);

            System.out.println("Данные в кэше (" + name + ") успешно загружены (при запуске или по расписанию).");

        } catch (Exception e) {
            System.err.println("Ошибка при загрузке кэша (при запуске или по расписанию) (" + name + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cacheDepoData() {
        String name = "Второй json";
        try {
            // 1. Получаем данные
            DepoFullDto data = fetchDataFromDepoService();

            // 2. Сериализуем данные в JSON
            String jsonData = objectMapper.writeValueAsString(data);

            // 3. Получаем текущее время
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            String formattedTime = now.format(formatter);

            // 4. Сохраняем данные в базу данных
            Optional<DataCache> existingEntry = dataCacheRepository.findByJsonName(name);
            DataCache cacheEntry;
            if (existingEntry.isPresent()) {
                cacheEntry = existingEntry.get();
            } else {
                cacheEntry = new DataCache();
                cacheEntry.setJsonName(name);
            }

            cacheEntry.setJson(jsonData);
            cacheEntry.setJsonTime(formattedTime);
            dataCacheRepository.save(cacheEntry);

            System.out.println("Данные в кэше (" + name + ") успешно загружены (при запуске или по расписанию).");

        } catch (Exception e) {
            System.err.println("Ошибка при загрузке кэша (при запуске или по расписанию) (" + name + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Метод для получения данных из AnalisService
    private AnalisFullDTO fetchDataFromAnalisService() {
        return analisService.getAllTransactions();
    }

    // Метод для получения данных из DepoService
    private DepoFullDto fetchDataFromDepoService() {
        return depoService.getTransactionsInWork();
    }

    private String getEmployeesCacheName(YearMonth yearMonth) {
        return "employees_" + yearMonth.toString();
    }

    private boolean checkIfCacheExists(String cacheName) {
        Optional<DataCache> existingEntry = dataCacheRepository.findByJsonName(cacheName);
        return existingEntry.isPresent();
    }

    private void cacheEmployeesDataForMonth(YearMonth yearMonth, List<EmployeesDto> employees) {
        String cacheName = getEmployeesCacheName(yearMonth);
        try {
            // 1. Преобразуем список сотрудников в JSON
            String jsonData = objectMapper.writeValueAsString(employees);

            // 2. Получаем текущее время
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            String formattedTime = now.format(formatter);

            // 3. Сохраняем данные в базу данных
            DataCache cacheEntry;
            if (checkIfCacheExists(cacheName)) {
                cacheEntry = dataCacheRepository.findByJsonName(cacheName).get();
            } else {
                cacheEntry = new DataCache();
                cacheEntry.setJsonName(cacheName);
            }

            cacheEntry.setJson(jsonData);
            cacheEntry.setJsonTime(formattedTime);
            dataCacheRepository.save(cacheEntry);

            System.out.println("Данные о сотрудниках за " + yearMonth + " сохранены в кэш: " + cacheName);

        } catch (Exception e) {
            System.err.println("Ошибка при кэшировании данных о сотрудниках за " + yearMonth + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateAndCacheEmployeesData() {
        YearMonth startMonth = YearMonth.of(2023, 1);
        YearMonth currentMonth = YearMonth.now();

        while (startMonth.isBefore(currentMonth.plusMonths(1))) {
            String cacheName = getEmployeesCacheName(startMonth);
            if (!checkIfCacheExists(cacheName)) {
                List<EmployeesDto> employees = employeesTableService.getUniqueEmployeeNamesBySpecialization(startMonth);
                cacheEmployeesDataForMonth(startMonth, employees);
            }
            startMonth = startMonth.plusMonths(1);
        }
    }

    private void updateCurrentMonthEmployeesCache() {
        YearMonth currentMonth = YearMonth.now();
        List<EmployeesDto> employees = employeesTableService.getUniqueEmployeeNamesBySpecialization(currentMonth);
        cacheEmployeesDataForMonth(currentMonth, employees);
    }
}