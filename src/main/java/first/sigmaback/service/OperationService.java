package first.sigmaback.service;

import first.sigmaback.entity.OperationNew;
import first.sigmaback.entity.OperationNorm;
import first.sigmaback.repository.OperationNewRepository;
import first.sigmaback.repository.OperationNormRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OperationService {

    private final OperationNewRepository operationNewRepository;
    private final OperationNormRepository operationNormRepository;

    public OperationService(OperationNewRepository operationNewRepository, OperationNormRepository operationNormRepository) {
        this.operationNewRepository = operationNewRepository;
        this.operationNormRepository = operationNormRepository;
    }

    public Map<String, String> calculateNormsByProfession(String transactionId) {
        // 1. Получаем все операции для данной транзакции из НОВОЙ таблицы
        List<OperationNew> operations = operationNewRepository.findByOperationTransaction(transactionId);

        // 2. Суммируем нормы и вычисляем время работы для каждой профессии
        double mechanicNormSum = 0;
        double electronNormSum = 0;
        double electricNormSum = 0;
        double techNormSum = 0;
        double mechanicOptionWorkTime = 0;
        double electronOptionWorkTime = 0;
        double electricOptionWorkTime = 0;
        double techOptionWorkTime = 0;

        // 3. Множество для отслеживания уже учтенных нормативов
        Set<String> processedNorms = new HashSet<>();

        for (OperationNew operation : operations) {
            // Пропускаем операции, у которых НЕТ start ИЛИ stop
            if (operation.getOperationStartWork() == null || operation.getOperationStopWork() == null) {
                continue;
            }

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

            // Если категория "операция", пропускаем (работаем только с опциями)
            if ("операция".equals(operationCategory)) {
                continue;
            }

            // Получаем тип (профессию) из OperationNorm
            String operationNormType = operationNorm.getNormType();

            // Получаем норму из OperationNorm
            String normString = operationNorm.getNorm();

            // Если normString null, пропускаем
            if (normString == null) {
                continue;
            }

            double norm = 0;

            // Пытаемся преобразовать строку в число
            try {
                norm = Double.parseDouble(normString);
            } catch (NumberFormatException e) {
                System.err.println("Не удалось преобразовать норму в число: " + normString);
                continue;
            }

            // Вычисляем время работы для данной ВАЛИДНОЙ операции
            double workTime = calculateWorkTime(operation.getOperationStartWork(), operation.getOperationStopWork());

            // Ключ для отслеживания уникальных нормативов
            String normKey = operationType + "_" + operationNormType;

            // Добавляем время работы ВСЕГДА (суммируем для всех ВАЛИДНЫХ записей)
            if ("Механик".equals(operationNormType)) {
                mechanicOptionWorkTime += workTime; // ← СУММИРУЕМ время работы
                // Норматив добавляем ТОЛЬКО ОДИН РАЗ
                if (!processedNorms.contains(normKey)) {
                    mechanicNormSum += norm; // ← норматив ТОЛЬКО ОДИН РАЗ
                    processedNorms.add(normKey);
                }
            } else if ("Электронщик".equals(operationNormType)) {
                electronOptionWorkTime += workTime;
                if (!processedNorms.contains(normKey)) {
                    electronNormSum += norm;
                    processedNorms.add(normKey);
                }
            } else if ("Электрик".equals(operationNormType)) {
                electricOptionWorkTime += workTime;
                if (!processedNorms.contains(normKey)) {
                    electricNormSum += norm;
                    processedNorms.add(normKey);
                }
            } else if ("Технолог".equals(operationNormType)) {
                techOptionWorkTime += workTime;
                if (!processedNorms.contains(normKey)) {
                    techNormSum += norm;
                    processedNorms.add(normKey);
                }
            }
        }

        // 4. Создаем карту для хранения результатов
        Map<String, String> results = new HashMap<>();

        // 5. Устанавливаем нормы и время работы для каждой профессии в карту
        results.put("Механик", String.valueOf(mechanicNormSum));
        results.put("Электронщик", String.valueOf(electronNormSum));
        results.put("Электрик", String.valueOf(electricNormSum));
        results.put("Технолог", String.valueOf(techNormSum));
        results.put("mechanicOption", formatWorkTime(mechanicOptionWorkTime));
        results.put("electronOption", formatWorkTime(electronOptionWorkTime));
        results.put("electricOption", formatWorkTime(electricOptionWorkTime));
        results.put("techOption", formatWorkTime(techOptionWorkTime));

        // Возвращаем результат
        return results;
    }

    // Остальные методы остаются без изменений
    public double calculateWorkTime(Timestamp startTime, Timestamp stopTime) {
        if (startTime == null || stopTime == null) {
            return 0.0;
        }

        LocalDateTime start = startTime.toLocalDateTime();
        LocalDateTime stop = stopTime.toLocalDateTime();

        if (start.toLocalDate().equals(stop.toLocalDate())) {
            return calculateWorkTimeSameDay(start, stop);
        } else {
            return calculateWorkTimeDifferentDays(start, stop);
        }
    }

    private double calculateWorkTimeSameDay(LocalDateTime start, LocalDateTime stop) {
        if (start.isAfter(stop)) {
            return 0.0;
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
            return 0.0;
        }

        Duration duration = Duration.between(start, stop);
        return duration.getSeconds() / 3600.0;
    }

    private double calculateWorkTimeDifferentDays(LocalDateTime start, LocalDateTime stop) {
        double totalWorkTime = 0.0;

        LocalDateTime current = start;
        while (current.isBefore(stop)) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                if (current.toLocalDate().equals(start.toLocalDate())) {
                    totalWorkTime += calculateWorkTimeStartDay(current, stop);
                } else if (current.toLocalDate().equals(stop.toLocalDate())) {
                    totalWorkTime += calculateWorkTimeStopDay(current, stop);
                } else {
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
            return 0.0;
        }

        Duration duration;
        if (stop.toLocalDate().equals(start.toLocalDate())) {
            duration = Duration.between(start, stop);
        } else {
            duration = Duration.between(start, workEndTime);
        }

        return duration.getSeconds() / 3600.0;
    }

    private double calculateWorkTimeStopDay(LocalDateTime current, LocalDateTime stop) {
        LocalTime workStart = LocalTime.of(8, 30);
        LocalDateTime workStartTime = current.toLocalDate().atTime(workStart);

        if (stop.isBefore(workStartTime)) {
            return 0.0;
        }

        Duration duration = Duration.between(workStartTime, stop);
        return duration.getSeconds() / 3600.0;
    }

    public String formatWorkTime(double workTime) {
        long totalSeconds = Math.round(workTime * 3600);
        long HH = totalSeconds / 3600;
        long MM = (totalSeconds % 3600) / 60;
        long SS = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", HH, MM, SS);
    }
}