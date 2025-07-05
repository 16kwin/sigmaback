package first.sigmaback.service;

import first.sigmaback.dto.AnalisDTO;
import first.sigmaback.dto.AnalisFullDTO;
import first.sigmaback.dto.AnalisHeaderDTO;
import first.sigmaback.entity.Ppp;
import first.sigmaback.repository.PppRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalisService {

    private final PppRepository pppRepository;
    private final AnalisHeaderService analisHeaderService;

    @Autowired
    public AnalisService(PppRepository pppRepository, AnalisHeaderService analisHeaderService) {
        this.pppRepository = pppRepository;
        this.analisHeaderService = analisHeaderService;
    }

    public AnalisFullDTO getAllTransactions() {
        // 1. Получаем данные для заголовка
        AnalisHeaderDTO header = analisHeaderService.getNorms();

        // 2. Получаем данные о транзакциях
        List<Ppp> ppps = pppRepository.findAll();

        // 3. Преобразуем Ppp в AnalisDTO
        List<AnalisDTO> analisDTOs = ppps.stream()
                .map(this::convertToAnalisDTO)
                .collect(Collectors.toList());

        // 4. Создаем AnalisFullDTO и заполняем данными
        AnalisFullDTO fullDTO = new AnalisFullDTO();
        fullDTO.setHeader(header);
        fullDTO.setTransactions(analisDTOs);

        return fullDTO;
    }

    private AnalisDTO convertToAnalisDTO(Ppp ppp) {
        AnalisDTO dto = new AnalisDTO();
        dto.setTransaction(ppp.getTransaction());
        dto.setStatus(ppp.getStatus());
        dto.setPlanPpp(ppp.getPlanPpp() * 8);
        dto.setPlanDateStart(ppp.getPlanDateStart());
        dto.setForecastDateStart(ppp.getForecastDateStart());
        dto.setFactDateStart(ppp.getFactDateStart());
        dto.setPlanDateStop(ppp.getPlanDateStop());
        dto.setForecastDateStop(ppp.getForecastDateStop());
        dto.setFactDateStop(ppp.getFactDateStop());
        dto.setPlanDateShipment(ppp.getPlanDateShipment());
        dto.setForecastDateShipment(ppp.getForecastDateShipment());
        dto.setFactDateShipment(ppp.getFactDateShipment());
        return dto;
    }
}