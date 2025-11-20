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
        
        // УБИРАЕМ ЛОГИРОВАНИЕ - просто возвращаем пустой Map
        if (planDateStart == null) {
            return dates;
        }
        
        LocalDate currentDate = planDateStart;

        for (int i = 1; i <= norms.size(); i++) {
            Double norm = norms.get(i);
            if (norm != null) {
                currentDate = calculateNewDate(currentDate, norm);
                dates.put(i, currentDate);
            } else {
                // УБИРАЕМ ЛОГИРОВАНИЕ для отсутствующих нормативов
                dates.put(i, currentDate);
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
                currentDateTime = currentDateTime.plusDays(1).with(LocalTime.of(8, 30));
                continue;
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
                currentDateTime = currentDateTime.plusDays(1).with(LocalTime.of(8, 30));
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