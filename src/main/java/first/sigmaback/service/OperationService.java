package first.sigmaback.service;

import first.sigmaback.entity.Operation;
import first.sigmaback.entity.OperationNorm;
import first.sigmaback.repository.OperationRepository;
import first.sigmaback.repository.OperationNormRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperationService {

    private final OperationRepository operationRepository;
    private final OperationNormRepository operationNormRepository;

    public OperationService(OperationRepository operationRepository, OperationNormRepository operationNormRepository) {
        this.operationRepository = operationRepository;
        this.operationNormRepository = operationNormRepository;
    }

    public Map<String, String> calculateNormsByProfession(String transactionId) {
        // 1. Получаем все операции для данной транзакции
        List<Operation> operations = operationRepository.findByOperationTransaction(transactionId);

        // 2. Суммируем нормы и вычисляем время работы для каждой профессии
        double mechanicNormSum = 0;
        double electronNormSum = 0;
        double electricNormSum = 0;
        double techNormSum = 0;
        double mechanicOptionWorkTime = 0;
        double electronOptionWorkTime = 0;
        double electricOptionWorkTime = 0;
        double techOptionWorkTime = 0;

        for (Operation operation : operations) {
            // Получаем тип операции
            String operationType = operation.getOperationType();

            // Если тип операции не указан, пропускаем
            if (operationType == null || operationType.isEmpty()) {
                continue;
            }

            // Ищем соответствующую запись в OperationNorm по имени
            OperationNorm operationNorm = operationNormRepository.findByNormName(operationType);

            // Если норма не найдена, пропускаем
            if (operationNorm == null) {
                continue;
            }

            // Получаем категорию из OperationNorm
            String operationCategory = operationNorm.getNormCategory();

            // Если категория "Опция", пропускаем
            if ("операция".equals(operationCategory)) {
                continue;
            }

            // Получаем тип (профессию) из OperationNorm
            String operationNormType = operationNorm.getNormType();

            // Получаем норму из OperationNorm
            String normString = operationNorm.getNorm();

            // Если normString null, пропускаем
            if (normString == null) {
                continue; // Пропускаем текущую операцию, если normString == null
            }

            double norm = 0; // Значение по умолчанию

            // Пытаемся преобразовать строку в число
            try {
                norm = Double.parseDouble(normString);
            } catch (NumberFormatException e) {
                // Обработка ошибки преобразования, например, логирование
                System.err.println("Не удалось преобразовать норму в число: " + normString);
                continue; // Пропускаем текущую операцию
            }

            // Вычисляем время работы для данной операции
            double workTime = calculateWorkTime(operation.getOperationStartWork(), operation.getOperationStopWork());

            // Добавляем норму и время работы к сумме для соответствующей профессии
            if ("Механик".equals(operationNormType)) {
                mechanicNormSum += norm;
                mechanicOptionWorkTime += workTime;
            } else if ("Электронщик".equals(operationNormType)) {
                electronNormSum += norm;
                electronOptionWorkTime += workTime;
            } else if ("Электрик".equals(operationNormType)) {
                electricNormSum += norm;
                electricOptionWorkTime += workTime;
            } else if ("Технолог".equals(operationNormType)) {
                techNormSum += norm;
                techOptionWorkTime += workTime;
            }
        }

        // 3. Создаем карту для хранения результатов
        Map<String, String> results = new HashMap<>(); // Изменен тип Map

        // 4. Устанавливаем сумму норм и время работы для каждой профессии в карту
        results.put("Механик", String.valueOf(mechanicNormSum)); // Преобразуем Double в String
        results.put("Электронщик", String.valueOf(electronNormSum)); // Преобразуем Double в String
        results.put("Электрик", String.valueOf(electricNormSum)); // Преобразуем Double в String
        results.put("Технолог", String.valueOf(techNormSum)); // Преобразуем Double в String
        results.put("mechanicOption", formatWorkTime(mechanicOptionWorkTime)); // Форматируем и добавляем
        results.put("electronOption", formatWorkTime(electronOptionWorkTime)); // Форматируем и добавляем
        results.put("electricOption", formatWorkTime(electricOptionWorkTime)); // Форматируем и добавляем
        results.put("techOption", formatWorkTime(techOptionWorkTime)); // Форматируем и добавляем

        // Возвращаем результат
        return results;
    }

    public double calculateWorkTime(Timestamp startTime, Timestamp stopTime) {
        if (startTime == null || stopTime == null) {
            return 0.0; // Если start или stop равны null, возвращаем 0
        }

        LocalDateTime start = startTime.toLocalDateTime();
        LocalDateTime stop = stopTime.toLocalDateTime();

        if (start.toLocalDate().equals(stop.toLocalDate())) {
            // Если start и stop в один день, просто вычисляем разницу
            return calculateWorkTimeSameDay(start, stop);
        } else {
            // Если в разные дни, используем сложную логику
            return calculateWorkTimeDifferentDays(start, stop);
        }
    }

    private double calculateWorkTimeSameDay(LocalDateTime start, LocalDateTime stop) {
        // Если start и stop в один день
        if (start.isAfter(stop)) {
            return 0.0; // Если start позже stop, возвращаем 0
        }

        LocalTime workStart = LocalTime.of(8, 30);
        LocalTime workEnd = LocalTime.of(17, 30);

        LocalDateTime workStartTime = start.toLocalDate().atTime(workStart);
        LocalDateTime workEndTime = start.toLocalDate().atTime(workEnd);

        if (start.isBefore(workStartTime)) {
            start = workStartTime;
        }

        if (stop.isAfter(workEndTime)) {
            stop = workEndTime;
        }

        if (start.isAfter(stop)) {
            return 0.0; // Если start позже stop после корректировки, возвращаем 0
        }

        Duration duration = Duration.between(start, stop);
        return duration.getSeconds() / 3600.0; // Возвращаем разницу в часах с точностью до секунд
    }

    private double calculateWorkTimeDifferentDays(LocalDateTime start, LocalDateTime stop) {
        double totalWorkTime = 0.0;

        LocalDateTime current = start;
        while (current.isBefore(stop)) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                // Если это рабочий день
                if (current.toLocalDate().equals(start.toLocalDate())) {
                    // Если это день start
                    totalWorkTime += calculateWorkTimeStartDay(current, stop);
                } else if (current.toLocalDate().equals(stop.toLocalDate())) {
                    // Если это день stop
                    totalWorkTime += calculateWorkTimeStopDay(current, stop);
                } else {
                    // Если это полный рабочий день
                    totalWorkTime += 8.0;
                }
            }
            current = current.plusDays(1);
        }

        return totalWorkTime;
    }

    private double calculateWorkTimeStartDay(LocalDateTime start, LocalDateTime stop) {
        LocalTime workEnd = LocalTime.of(17, 30);
        LocalDateTime workEndTime = start.toLocalDate().atTime(workEnd);

        if (start.isAfter(workEndTime)) {
            return 0.0; // Если start позже 17:30, возвращаем 0
        }

        Duration duration;
        if (stop.toLocalDate().equals(start.toLocalDate())) {
            duration = Duration.between(start, stop);
        } else {
            duration = Duration.between(start, workEndTime);
        }

        return duration.getSeconds() / 3600.0; // Возвращаем разницу в часах с точностью до секунд
    }

    private double calculateWorkTimeStopDay(LocalDateTime current, LocalDateTime stop) {
        LocalTime workStart = LocalTime.of(8, 30);
        LocalDateTime workStartTime = current.toLocalDate().atTime(workStart);

        if (stop.isBefore(workStartTime)) {
            return 0.0; // Если stop раньше 8:30, возвращаем 0
        }

        Duration duration;
         duration = Duration.between(workStartTime, stop);

        return duration.getSeconds() / 3600.0; // Возвращаем разницу в часах с точностью до секунд
    }

public String formatWorkTime(double workTime) {
    long totalSeconds = Math.round(workTime * 3600); // Преобразуем часы в секунды
    long HH = totalSeconds / 3600;
    long MM = (totalSeconds % 3600) / 60;
    long SS = totalSeconds % 60;
    return String.format("%02d:%02d:%02d", HH, MM, SS);
}
}