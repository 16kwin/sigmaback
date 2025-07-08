package first.sigmaback.controller;

import first.sigmaback.service.DataCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analis")
@CrossOrigin(origins = "http://localhost:3000")
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
}