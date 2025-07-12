package first.sigmaback.service;

import first.sigmaback.entity.Operation;
import first.sigmaback.repository.OperationRepository;
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

    private final OperationRepository operationRepository;
    private final OperationService operationService; // Для вычисления времени

    public TimeService(OperationRepository operationRepository, OperationService operationService) {
        this.operationRepository = operationRepository;
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

        // Получаем все операции для данной транзакции
        List<Operation> operations = operationRepository.findByOperationTransaction(transactionId);

        // Создаем карту для хранения результатов
        Map<String, Map<String, String>> results = new HashMap<>();

        // Для каждой операции из списка
        for (String operationName : operationNames) {
            // Ищем операцию в списке полученных операций
            Operation foundOperation = null;
            for (Operation operation : operations) {
                if (operationName.equals(operation.getOperationType())) {
                    foundOperation = operation;
                    break;
                }
            }

            // Создаем карту для хранения информации об операции
Map<String, String> operationInfo = new HashMap<>();

// Если операция найдена
if (foundOperation != null) {
    // Форматируем startTime и stopTime
    Timestamp startTime = foundOperation.getOperationStartWork();
    Timestamp stopTime = foundOperation.getOperationStopWork();

    String startTimeString = (startTime != null) ? formatTimestamp(startTime) : "Нет данных";
    String stopTimeString = (stopTime != null) ? formatTimestamp(stopTime) : "Нет данных";

    operationInfo.put("startTime", startTimeString);
    operationInfo.put("stopTime", stopTimeString);

    // Вычисляем время выполнения
    double workTime = operationService.calculateWorkTime(startTime, stopTime);

    // Форматируем время выполнения
    String workTimeString = operationService.formatWorkTime(workTime);

    operationInfo.put("workTime", workTimeString);
} else {
    // Если операция не найдена
    operationInfo.put("startTime", "Нет данных");
    operationInfo.put("stopTime", "Нет данных");
    operationInfo.put("workTime", "Нет данных"); // Или тоже "Нет данных", если хотите
}

// Добавляем информацию об операции в карту результатов
results.put(operationName, operationInfo);
        }

        // Возвращаем результат
        return results;
    }

    // Метод для форматирования Timestamp в строку "yyyy-MM-dd HH:mm:ss"
    private String formatTimestamp(Timestamp timestamp) {
        LocalDateTime localDateTime = timestamp.toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return localDateTime.format(formatter);
    }
}