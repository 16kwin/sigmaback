package first.sigmaback.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class InterService {

    // Время начала и окончания рабочего дня
    private static final LocalTime WORK_DAY_START = LocalTime.of(8, 30);
    private static final LocalTime WORK_DAY_END = LocalTime.of(17, 30);
    private static final int WORK_DAY_HOURS = 8;

    private static final String NO_DATA = "Нет данных";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String calculateTimeBetweenOperations(String startTimeString, String endTimeString) {
        if (NO_DATA.equals(startTimeString) || NO_DATA.equals(endTimeString)) {
            return NO_DATA;
        }

        LocalDateTime startTime;
        LocalDateTime endTime;

        try {
            startTime = LocalDateTime.parse(startTimeString, FORMATTER);
            endTime = LocalDateTime.parse(endTimeString, FORMATTER);
        } catch (DateTimeParseException e) {
            System.err.println("Ошибка парсинга даты: " + e.getMessage());
            return NO_DATA; // Или можно бросить исключение
        }

        if (startTime == null || endTime == null) {
            return NO_DATA; // Или можно бросить исключение
        }

        Duration totalDuration = Duration.between(startTime, endTime);
        long totalDays = totalDuration.toDays();

        double totalHours = 0.0;

        LocalDateTime current = startTime;
        for (int i = 0; i <= totalDays; i++) {
            if (isWeekend(current)) {
                current = current.plusDays(1);
                continue;
            }

            if (current.toLocalDate().equals(startTime.toLocalDate())) {
                // Первый день
                totalHours += calculatePartialDayHours(startTime, WORK_DAY_END.atDate(startTime.toLocalDate()));
            } else if (current.toLocalDate().equals(endTime.toLocalDate())) {
                // Последний день
                totalHours += calculatePartialDayHours(WORK_DAY_START.atDate(endTime.toLocalDate()), endTime);
            } else {
                // Полный рабочий день
                totalHours += WORK_DAY_HOURS;
            }

            current = current.plusDays(1);
        }

        // Форматируем результат в "HH:mm:ss"
        long hours = (long) totalHours;
        long minutes = (long) ((totalHours - hours) * 60);
        long seconds = (long) (((totalHours - hours) * 60 - minutes) * 60);

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private double calculatePartialDayHours(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            return 0.0;
        }
        Duration duration = Duration.between(start, end);
        return (double) duration.toMinutes() / 60.0;
    }

    private boolean isWeekend(LocalDateTime date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}