package first.sigmaback.service;

import first.sigmaback.entity.OperationNew;
import first.sigmaback.repository.OperationNewRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class InterService {

    private final OperationNewRepository operationNewRepository;

    public InterService(OperationNewRepository operationNewRepository) {
        this.operationNewRepository = operationNewRepository;
    }

    // Время начала и окончания рабочего дня
    private static final LocalTime WORK_DAY_START = LocalTime.of(8, 30);
    private static final LocalTime WORK_DAY_END = LocalTime.of(17, 30);

    private static final String NO_DATA = "Нет данных";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String calculateTimeBetweenOperations(String stagePpp) {
        // Получаем все операции для данной stage_ppp
        List<OperationNew> operations = operationNewRepository.findByStagePpp(stagePpp);

        if (operations.isEmpty()) {
            return NO_DATA;
        }

        // Группируем по stage_ppp и находим min start и max stop
        LocalDateTime minStart = null;
        LocalDateTime maxStop = null;

        for (OperationNew operation : operations) {
            if (operation.getOperationStartWork() != null) {
                LocalDateTime start = operation.getOperationStartWork().toLocalDateTime();
                if (minStart == null || start.isBefore(minStart)) {
                    minStart = start;
                }
            }

            if (operation.getOperationStopWork() != null) {
                LocalDateTime stop = operation.getOperationStopWork().toLocalDateTime();
                if (maxStop == null || stop.isAfter(maxStop)) {
                    maxStop = stop;
                }
            }
        }

        if (minStart == null || maxStop == null) {
            return NO_DATA;
        }

        // Используем существующую логику расчета времени
        return calculateTimeBetweenOperations(minStart.format(FORMATTER), maxStop.format(FORMATTER));
    }

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
            return NO_DATA;
        }

        if (startTime == null || endTime == null || startTime.isAfter(endTime)) {
            return NO_DATA;
        }

        // Точный расчет в секундах
        long totalSeconds = 0;
        LocalDateTime current = startTime;

        while (current.isBefore(endTime)) {
            if (isWeekend(current)) {
                current = current.plusDays(1).with(WORK_DAY_START);
                continue;
            }

            LocalDateTime workDayStart = current.toLocalDate().atTime(WORK_DAY_START);
            LocalDateTime workDayEnd = current.toLocalDate().atTime(WORK_DAY_END);

            // Начало периода для текущего дня
            LocalDateTime periodStart = current.isAfter(workDayStart) ? current : workDayStart;
            
            // Конец периода для текущего дня
            LocalDateTime periodEnd = endTime.isBefore(workDayEnd) ? endTime : workDayEnd;

            if (periodStart.isBefore(periodEnd)) {
                Duration dayDuration = Duration.between(periodStart, periodEnd);
                totalSeconds += dayDuration.getSeconds();
            }

            // Переход к следующему рабочему дню
            current = current.plusDays(1).with(WORK_DAY_START);
        }

        // Точное преобразование секунд в HH:mm:ss
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private boolean isWeekend(LocalDateTime date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}