package first.sigmaback.service;

import first.sigmaback.entity.Employees;
import first.sigmaback.entity.Problems;
import first.sigmaback.repository.EmployeesRepository;
import first.sigmaback.repository.ProblemsRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProblemService {

    private final ProblemsRepository problemsRepository;
    private final EmployeesRepository employeesRepository;

    public ProblemService(ProblemsRepository problemsRepository, EmployeesRepository employeesRepository) {
        this.problemsRepository = problemsRepository;
        this.employeesRepository = employeesRepository;
    }

    public Map<String, Double> getProblemHoursByProfession(String transactionId) {
        // 1. Получаем все проблемы для данной транзакции
        List<Problems> problems = problemsRepository.findByProblemsTransaction(transactionId);

        // 2. Фильтруем проблемы, оставляя только те, у которых указан сотрудник
        List<Problems> filteredProblems = problems.stream()
                .filter(problem -> problem.getProblemsEmployee() != null && !problem.getProblemsEmployee().isEmpty())
                .collect(Collectors.toList());

        // 3. Создаем Map для хранения суммарных часов по профессиям
        Map<String, Double> problemHoursByProfession = new HashMap<>();

        // 4. Итерируемся по отфильтрованным проблемам
        for (Problems problem : filteredProblems) {
            // 5. Получаем сотрудника по имени
            Employees employee = employeesRepository.findFirstByEmployeesName(problem.getProblemsEmployee());

            // 6. Если сотрудник найден
            if (employee != null) {
                // 7. Получаем специализацию (профессию) сотрудника
                String specialization = employee.getEmployeesSpecialization();

                // 8. Получаем часы проблемы
                Double problemHours = problem.getProblemsHours();

                // 9. Если часы не указаны, считаем их равными 0
                if (problemHours == null) {
                    problemHours = 0.0;
                }

                // 10. Добавляем часы к сумме для данной профессии
                problemHoursByProfession.merge(specialization, problemHours, Double::sum);
            }
        }

        return problemHoursByProfession;
    }
}