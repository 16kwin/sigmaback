package first.sigmaback.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class DatesService {

    public Map<Integer, LocalDate> calculateDates(LocalDate planDateStart, Map<Integer, Double> norms) {
        Map<Integer, LocalDate> dates = new HashMap<>();
        LocalDate currentDate = planDateStart;

        for (int i = 1; i <= norms.size(); i++) {
            Double norm = norms.get(i);
            if (norm != null) {
                currentDate = calculateNewDate(currentDate, norm); // Используем calculateNewDate
                dates.put(i, currentDate);
            } else {
                // Обработка отсутствия норматива для этапа
                System.err.println("Предупреждение: Отсутствует норматив для этапа " + i + ".  Пропуск этапа.");
                dates.put(i, currentDate); // Или бросать исключение, если это недопустимо
            }
        }

        return dates;
    }


    public LocalDate calculateNewDate(LocalDate startDate, double hoursToAdd) {
        // 1. Преобразование часов в минуты
        long totalMinutesToAdd = (long) (hoursToAdd * 60);

        // 2. Инициализация currentDateTime
        LocalDateTime currentDateTime = LocalDateTime.of(startDate, LocalTime.of(8, 30));

        // 3. Итеративное добавление времени
        while (totalMinutesToAdd > 0) {
            // Проверка на выходной
            if (isWeekend(currentDateTime.toLocalDate())) {
                currentDateTime = currentDateTime.plusDays(1).with(LocalTime.of(8, 30)); // Переходим к 8:30 следующего дня
                continue; // Переходим к следующей итерации цикла
            }

            // Определение рабочего времени
            LocalTime startTime = LocalTime.of(8, 30);
            LocalTime endTime = LocalTime.of(17, 30);
            long availableMinutes = ChronoUnit.MINUTES.between(startTime, endTime);

            // Добавление времени
            if (totalMinutesToAdd <= availableMinutes) {
                currentDateTime = currentDateTime.plusMinutes(totalMinutesToAdd);
                totalMinutesToAdd = 0;
            } else {
                currentDateTime = currentDateTime.plusMinutes(availableMinutes);
                totalMinutesToAdd -= availableMinutes;
                currentDateTime = currentDateTime.plusDays(1).with(LocalTime.of(8, 30)); // Переходим к 8:30 следующего дня
            }
        }

        // 4. Возврат только даты
        return currentDateTime.toLocalDate();
    }

    // Helper method to check if a date is a weekend
    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}