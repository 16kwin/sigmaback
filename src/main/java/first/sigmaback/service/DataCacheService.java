package first.sigmaback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import first.sigmaback.dto.AnalisFullDTO;
import first.sigmaback.entity.DataCache;
import first.sigmaback.repository.DataCacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class DataCacheService {

    private final DataCacheRepository dataCacheRepository;
    private final ObjectMapper objectMapper;
    private final AnalisService analisService;

    @Autowired //  Добавляем аннотацию @Autowired к конструктору
    public DataCacheService(DataCacheRepository dataCacheRepository, ObjectMapper objectMapper, AnalisService analisService) {
        this.dataCacheRepository = dataCacheRepository;
        this.objectMapper = objectMapper;
        this.analisService = analisService;
        loadCache(); // Загружаем данные при создании сервиса
    }

    // Получение данных (запрос)
    public String getCachedData(String name) {
        Optional<DataCache> cacheEntry = dataCacheRepository.findByJsonName(name);
        return cacheEntry.map(DataCache::getJson).orElse(null);
    }

    // Загрузка кэша при запуске приложения и по расписанию
    @Scheduled(fixedRate = 1800000) // Запускать каждые 30 минут (1800000 миллисекунд)
    @Transactional
    public void loadCache() {
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

    // Метод для получения данных из AnalisService
    private AnalisFullDTO fetchDataFromAnalisService() {
        return analisService.getAllTransactions();
    }
}