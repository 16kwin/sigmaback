package first.sigmaback.service;

import first.sigmaback.entity.Operation;
import first.sigmaback.entity.OperationNorm;
import first.sigmaback.repository.OperationRepository;
import first.sigmaback.repository.OperationNormRepository;
import org.springframework.stereotype.Service;

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

    public Map<String, Double> calculateNormsByProfession(String transactionId) {
        // 1. Получаем все операции для данной транзакции
        List<Operation> operations = operationRepository.findByOperationTransaction(transactionId);

        // 2. Суммируем нормы для каждой профессии
        double mechanicNormSum = 0;
        double electronNormSum = 0;
        double electricNormSum = 0;
        double techNormSum = 0;

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

            // Добавляем норму к сумме для соответствующей профессии
            if ("Механик".equals(operationNormType)) {
                mechanicNormSum += norm;
            } else if ("Электронщик".equals(operationNormType)) {
                electronNormSum += norm;
            } else if ("Электрик".equals(operationNormType)) {
                electricNormSum += norm;
            } else if ("Технолог".equals(operationNormType)) {
                techNormSum += norm;
            }
        }

        // 3. Создаем карту для хранения результатов
        Map<String, Double> normsByProfession = new HashMap<>();

        // 4. Устанавливаем сумму норм для каждой профессии в карту
        normsByProfession.put("Механик", mechanicNormSum);
        normsByProfession.put("Электронщик", electronNormSum);
        normsByProfession.put("Электрик", electricNormSum);
        normsByProfession.put("Технолог", techNormSum);

        // Возвращаем результат
        return normsByProfession;
    }
}