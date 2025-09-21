package first.sigmaback.controller;

import first.sigmaback.service.DataCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/analis")
@CrossOrigin(origins = {"http://194.87.56.253:3000", "http://194.87.56.253:3001"})
public class AnalisController {

    private final DataCacheService dataCacheService;

    @Autowired
    public AnalisController(DataCacheService dataCacheService) {
        this.dataCacheService = dataCacheService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<String> getAllTransactions() {
        String cachedJson = dataCacheService.getCachedData("Первый json"); // Получаем JSON из кэша
        if (cachedJson != null) {
            return ResponseEntity.ok(cachedJson); // Возвращаем JSON
        } else {
            return ResponseEntity.notFound().build(); // Если кэш пуст
        }
    }

    @GetMapping("/depo")
    public ResponseEntity<String> getDepoTransactions() { // Изменяем имя метода
        String cachedJson = dataCacheService.getCachedData("Второй json"); // Получаем JSON из кэша
        if (cachedJson != null) {
            return ResponseEntity.ok(cachedJson); // Возвращаем JSON
        } else {
            return ResponseEntity.notFound().build(); // Если кэш пуст
        }
    }

    @GetMapping("/employees")
    public ResponseEntity<String> getEmployees(@RequestParam(value = "month", required = false) String month) {
        YearMonth yearMonth;
        if (month == null || month.isEmpty()) {
            // Если параметр month не указан, используем текущий месяц
            yearMonth = YearMonth.now();
        } else {
            try {
                yearMonth = YearMonth.parse(month); //  Пытаемся спарсить параметр month
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body("Неверный формат месяца. Используйте формат YYYY-MM."); // Если формат неверный
            }
        }

        String cacheName = "employees_" + yearMonth.toString(); // Формируем имя кэша
        String cachedJson = dataCacheService.getCachedData(cacheName); // Получаем JSON из кэша

        if (cachedJson != null) {
            return ResponseEntity.ok(cachedJson); // Возвращаем JSON
        } else {
            return ResponseEntity.notFound().build(); // Если кэш пуст
        }
    }
}