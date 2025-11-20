package first.sigmaback.service;

import first.sigmaback.entity.OperationNew;
import first.sigmaback.repository.OperationNewRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TimeService {

    private final OperationNewRepository operationNewRepository;
    private final OperationService operationService;

    public TimeService(OperationNewRepository operationNewRepository, OperationService operationService) {
        this.operationNewRepository = operationNewRepository;
        this.operationService = operationService;
    }

    public Map<String, Map<String, String>> calculateOperationTimes(String transactionId) {
        // Список операций, для которых нужно вычислить время
        List<String> operationNames = Arrays.asList(
                "Входной контроль",
                "Подключение",
                "Проверка механиком",
                "Проверка электронщиком",
                "Проверка технологом",
                "Выходной контроль",
                "Транспортное положение"
        );

        // Получаем все операции для данной транзакции из НОВОЙ таблицы
        List<OperationNew> operations = operationNewRepository.findByOperationTransaction(transactionId);

        // Создаем карту для хранения результатов
        Map<String, Map<String, String>> results = new HashMap<>();

        // Для каждой операции из списка
        for (String operationName : operationNames) {
            // Создаем карту для хранения информации об операции
            Map<String, String> operationInfo = new HashMap<>();

            // Ищем ВСЕ операции данного типа в списке
            List<OperationNew> foundOperations = operations.stream()
                    .filter(operation -> operationName.equals(operation.getOperationType()))
                    .toList();

            // Фильтруем только те операции, у которых ЕСТЬ И start И stop
            List<OperationNew> validOperations = foundOperations.stream()
                    .filter(operation -> operation.getOperationStartWork() != null && operation.getOperationStopWork() != null)
                    .toList();

            // Если найдены ВАЛИДНЫЕ операции данного типа
            if (!validOperations.isEmpty()) {
                // Суммируем время всех ВАЛИДНЫХ операций данного типа
                double totalWorkTime = 0.0;
                boolean hasValidStartTime = false;
                boolean hasValidStopTime = false;
                
                String earliestStartTime = null;
                String latestStopTime = null;
 
                for (OperationNew operation : validOperations) {
                    Timestamp startTime = operation.getOperationStartWork();
                    Timestamp stopTime = operation.getOperationStopWork();

                    // Вычисляем время выполнения для каждой ВАЛИДНОЙ операции
                    double workTime = operationService.calculateWorkTime(startTime, stopTime);
                    totalWorkTime += workTime;

                    // Определяем самый ранний startTime и самый поздний stopTime
                    if (startTime != null) {
                        String currentStartTime = formatTimestamp(startTime);
                        if (earliestStartTime == null || currentStartTime.compareTo(earliestStartTime) < 0) {
                            earliestStartTime = currentStartTime;
                        }
                        hasValidStartTime = true;
                    }

                    if (stopTime != null) {
                        String currentStopTime = formatTimestamp(stopTime);
                        if (latestStopTime == null || currentStopTime.compareTo(latestStopTime) > 0) {
                            latestStopTime = currentStopTime;
                        }
                        hasValidStopTime = true;
                    }
                }

                // Форматируем суммарное время выполнения
                String workTimeString = operationService.formatWorkTime(totalWorkTime);

                // Устанавливаем значения в operationInfo
                operationInfo.put("startTime", hasValidStartTime ? earliestStartTime : "Нет данных");
                operationInfo.put("stopTime", hasValidStopTime ? latestStopTime : "Нет данных");
                operationInfo.put("workTime", workTimeString);

            } else {
                // Если ВАЛИДНЫХ операций не найдено
                operationInfo.put("startTime", "Нет данных");
                operationInfo.put("stopTime", "Нет данных");
                operationInfo.put("workTime", "Нет данных");
            }

            // Добавляем информацию об операции в карту результатов
            results.put(operationName, operationInfo);
        }

        // Возвращаем результат
        return results;
    }

    // Метод для форматирования Timestamp в строку "yyyy-MM-dd HH:mm:ss"
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "Нет данных";
        }
        LocalDateTime localDateTime = timestamp.toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return localDateTime.format(formatter);
    }
}