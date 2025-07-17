package first.sigmaback.service;

import first.sigmaback.dto.AnalisDTO;
import first.sigmaback.dto.DepoDto;
import first.sigmaback.dto.DepoFullDto;
import first.sigmaback.dto.DepoHeaderDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DepoService {

    private final AnalisService analisService;

    @Autowired
    public DepoService(AnalisService analisService) {
        this.analisService = analisService;
    }

    public DepoFullDto getTransactionsInWork() {
        // 1. Получаем все транзакции из AnalisService
        List<AnalisDTO> allTransactions = analisService.getAllTransactions().getTransactions();

        // 2. Фильтруем транзакции, оставляя только те, у которых статус "В работе"
        List<AnalisDTO> transactionsInWork = allTransactions.stream()
                .filter(transaction -> "В работе".equals(transaction.getStatus())) // Замените "В работе" на актуальный статус, если он другой
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
        // totalOperationsWorkTimeSum - это String, представляющий часы
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
            // Парсим totalOperationsWorkTimeSum (который уже является часами в String) в int
            totalOperationsWorkTimeHours = Integer.parseInt(totalOperationsWorkTimeSum);
        } catch (NumberFormatException e) {
            System.err.println("Ошибка при парсинге totalOperationsWorkTimeSum в int: " + totalOperationsWorkTimeSum);
            // Если парсинг не удался, будем считать, что времени 0
            totalOperationsWorkTimeHours = 0;
        }

        int planPppDiff = 0;
        String planPppDiffPercentage = "Нет данных";

        // Только если planPppSum имеет значение, рассчитываем разницу и процент
        if (planPppSum != null && planPppSum != 0) {
            planPppDiff = Math.abs(planPppSum - totalOperationsWorkTimeHours);

            // Рассчитываем процент, делим на planPppSum
            // Проверяем, что planPppSum не равен нулю, чтобы избежать деления на ноль
            double percentage = ((double) totalOperationsWorkTimeHours / planPppSum) * 100;
            planPppDiffPercentage = String.format("%.2f", percentage).replace('.', ',');
        } else if (planPppSum == null) {
            // Если planPppSum null, разница тоже "Нет данных"
            planPppDiffPercentage = "Нет данных";
            planPppDiff = 0; // Или можно установить какое-то другое значение по умолчанию, если требуется
        }

        // 7.1. Считаем количество "нет" операций
        int noOperationsCount = 0;
        for (DepoDto depo : depoTransactions) {
            if ("нет".equals(depo.getVhodControlTimeExceeded())) {
                noOperationsCount++;
            }
            if ("нет".equals(depo.getElectricTimeExceeded())) {
                noOperationsCount++;
            }
            if ("нет".equals(depo.getMechanicTimeExceeded())) {
                noOperationsCount++;
            }
            if ("нет".equals(depo.getElectronTimeExceeded())) {
                noOperationsCount++;
            }
            if ("нет".equals(depo.getTechTimeExceeded())) {
                noOperationsCount++;
            }
            if ("нет".equals(depo.getVihodControlTimeExceeded())) {
                noOperationsCount++;
            }
            if ("нет".equals(depo.getTransportTimeExceeded())) {
                noOperationsCount++;
            }
        }

        // 8. Создаем DepoHeaderDto и заполняем его данными
        // Обратите внимание: теперь createDepoHeaderDto принимает 6 параметров
        DepoHeaderDto header = createDepoHeaderDto(
                transactionsInWork.size(),
                planPppSum,
                totalOperationsWorkTimeSum,
                totalTimeBetweenOperationsHours,
                planPppDiff,
                planPppDiffPercentage,
                noOperationsCount // Передаем подсчитанное количество "нет"
        );

        // 9. Создаем DepoFullDto и устанавливаем в него header и транзакции
        DepoFullDto depoFullDto = new DepoFullDto();
        depoFullDto.setHeader(header);
        depoFullDto.setTransactions(depoTransactions);

        return depoFullDto;
    }

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

        try {
            // Извлекаем числовое значение из строки (например, "26,13%")
            double percentage = Double.parseDouble(exceededTime.replace("%", "").replace(",", "."));

            if (percentage >= 100.0) {
                return "да";
            } else {
                return "нет";
            }
        } catch (NumberFormatException e) {
            // Обработка ошибки, если строка не может быть преобразована в число
            System.err.println("Ошибка при преобразовании строки в число: " + exceededTime);
            return "Ошибка"; // Или другое значение по умолчанию
        }
    }

    // Метод для создания DepoHeaderDto
    // Добавлены новые параметры: planPppDiff, planPppDiffPercentage, noOperationsCount
    private DepoHeaderDto createDepoHeaderDto(long totalTransactionsInWork, Integer planPppSum, String totalOperationsWorkTimeSum, int totalTimeBetweenOperationsHours, int planPppDiff, String planPppDiffPercentage, int noOperationsCount) {
        DepoHeaderDto header = new DepoHeaderDto();
        header.setTotalTransactionsInWork(totalTransactionsInWork);
        header.setPlanPppSum(planPppSum);
        header.setTotalOperationsWorkTimeSum(totalOperationsWorkTimeSum);
        header.setTotalTimeBetweenOperationsHours(totalTimeBetweenOperationsHours);
        header.setPlanPppDiff(planPppDiff);
        header.setPlanPppDiffPercentage(planPppDiffPercentage);
        header.setNoOperationsCount(noOperationsCount); // Устанавливаем новое поле
        return header;
    }

    // Метод для преобразования времени в секунды
    private long parseTimeToSeconds(String time) {
        if (time == null || time.isEmpty()) {
            return 0; // Или другое значение по умолчанию
        }

        String[] parts = time.split(":");
        if (parts.length != 3) {
            System.err.println("Неверный формат времени: " + time);
            return 0; // Или выбрасываем исключение, если формат неправильный
        }

        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            return hours * 3600L + minutes * 60 + seconds;
        } catch (NumberFormatException e) {
            System.err.println("Ошибка при парсинге времени: " + time);
            return 0; // Или выбрасываем исключение
        }
    }
}