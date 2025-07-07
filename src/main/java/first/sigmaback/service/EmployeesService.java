package first.sigmaback.service;

import first.sigmaback.entity.Operation;
import first.sigmaback.repository.OperationRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeesService {

    private final OperationRepository operationRepository;

    public EmployeesService(OperationRepository operationRepository) {
        this.operationRepository = operationRepository;
    }

    public Map<String, String> getEmployeeNamesForTransaction(String transactionId) {
        // Список операций, для которых нужно получить фамилии
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
        Map<String, String> employeeNames = new HashMap<>();

        // Для каждой операции из списка
        for (String operationName : operationNames) {
            String employeeName = ""; // Значение по умолчанию: пустая строка

            // Ищем операцию в списке полученных операций
            for (Operation operation : operations) {
                if (operationName.equals(operation.getOperationType())) {
                    // Если операция найдена и есть фамилия сотрудника
                    if (operation.getOperationEmployee() != null && !operation.getOperationEmployee().trim().isEmpty()) {
                        employeeName = operation.getOperationEmployee(); // Получаем фамилию сотрудника
                    }
                    break; // Прерываем поиск, так как нашли операцию
                }
            }

            // Добавляем фамилию сотрудника (или пустую строку) в карту результатов
            employeeNames.put(operationName, employeeName);
        }

        // Возвращаем карту с результатами
        return employeeNames;
    }
}