package first.sigmaback.controller;

import first.sigmaback.dto.AnalisFullDTO;
import first.sigmaback.service.AnalisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analis")
public class AnalisController {

    private final AnalisService analisService;

    @Autowired // Добавьте аннотацию @Autowired
    public AnalisController(AnalisService analisService) {
        this.analisService = analisService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<AnalisFullDTO> getAllTransactions() { // Измените возвращаемый тип и название метода
        AnalisFullDTO analisFullDTO = analisService.getAllTransactions(); // Получите AnalisFullDTO
        return ResponseEntity.ok(analisFullDTO); // Верните AnalisFullDTO
    }
}