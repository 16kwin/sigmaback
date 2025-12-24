package first.sigmaback.service;

import first.sigmaback.dto.AnalisHeaderDTO;
import first.sigmaback.entity.OperationNorm;
import first.sigmaback.repository.OperationNormRepository;
import first.sigmaback.repository.EmployeesRepository;
import first.sigmaback.repository.PppRepository;
import first.sigmaback.repository.OperationNewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalisHeaderService {

    private final OperationNormRepository operationNormRepository;
    private final EmployeesRepository employeesRepository;
    private final PppRepository pppRepository;
    private final OperationNewRepository operationNewRepository;

    public AnalisHeaderService(OperationNormRepository operationNormRepository, 
                               EmployeesRepository employeesRepository, 
                               PppRepository pppRepository,
                               OperationNewRepository operationNewRepository) {
        this.operationNormRepository = operationNormRepository;
        this.employeesRepository = employeesRepository;
        this.pppRepository = pppRepository;
        this.operationNewRepository = operationNewRepository;
    }

    public AnalisHeaderDTO getNorms() {
        System.out.println("[AnalisHeaderService] === НАЧАЛО getNorms() ===");
        long startTime = System.currentTimeMillis();
        
        try {
            // Получаем нормативы для операций
            System.out.println("[AnalisHeaderService] Получение нормативов для операций...");
            long normStartTime = System.currentTimeMillis();
            
            OperationNorm VhodControl = operationNormRepository.findById("Входной контроль").orElse(null);
            OperationNorm Podklyuchenie = operationNormRepository.findById("Подключение").orElse(null);
            OperationNorm MechOperation = operationNormRepository.findById("Проверка механиком").orElse(null);
            OperationNorm ElectronOperation = operationNormRepository.findById("Проверка электронщиком").orElse(null);
            OperationNorm TechOperation = operationNormRepository.findById("Проверка технологом").orElse(null);
            OperationNorm Vihod = operationNormRepository.findById("Выходной контроль").orElse(null);
            OperationNorm Transport = operationNormRepository.findById("Транспортное положение").orElse(null);
            
            long normTime = System.currentTimeMillis() - normStartTime;
            System.out.println("[AnalisHeaderService] Нормативы получены за " + normTime + " мс");

            // Получаем количество уникальных работников разных профессий (Всего)
            System.out.println("[AnalisHeaderService] Получение списков сотрудников по специальностям...");
            long employeesStartTime = System.currentTimeMillis();
            
            List<String> mechanics = employeesRepository.findDistinctEmployeesNameByEmployeesSpecializationIgnoreCase("Механик");
            List<String> electrons = employeesRepository.findDistinctEmployeesNameByEmployeesSpecializationIgnoreCase("Электронщик");
            List<String> techs = employeesRepository.findDistinctEmployeesNameByEmployeesSpecializationIgnoreCase("Технолог");
            List<String> electrics = employeesRepository.findDistinctEmployeesNameByEmployeesSpecializationIgnoreCase("Электрик");
            List<String> complections = employeesRepository.findDistinctEmployeesNameByEmployeesSpecializationIgnoreCase("Комплектация");
            
            long employeesTime = System.currentTimeMillis() - employeesStartTime;
            System.out.println("[AnalisHeaderService] Списки сотрудников получены за " + employeesTime + " мс");
            
            long mechanicCount = mechanics.size();
            long electronCount = electrons.size();
            long techCount = techs.size();
            long elecCount = electrics.size();
            long conplectCount = complections.size();
            
            System.out.println("[AnalisHeaderService] Статистика по сотрудникам:");
            System.out.println("  - Механики: " + mechanicCount);
            System.out.println("  - Электронщики: " + electronCount);
            System.out.println("  - Технологи: " + techCount);
            System.out.println("  - Электрики: " + elecCount);
            System.out.println("  - Комплектация: " + conplectCount);

            // Получаем количество занятых сотрудников по специальностям (оптимизированно)
            System.out.println("[AnalisHeaderService] Подсчет занятых сотрудников...");
            long busyStartTime = System.currentTimeMillis();
            
            int mechanicBusy = countBusyEmployeesFromList(mechanics, "Механик");
            int electronBusy = countBusyEmployeesFromList(electrons, "Электронщик");
            int techBusy = countBusyEmployeesFromList(techs, "Технолог");
            int elecBusy = countBusyEmployeesFromList(electrics, "Электрик");
            int conplectBusy = countBusyEmployeesFromList(complections, "Комплектация");
            
            long busyTime = System.currentTimeMillis() - busyStartTime;
            System.out.println("[AnalisHeaderService] Занятые сотрудники подсчитаны за " + busyTime + " мс");
            System.out.println("[AnalisHeaderService] Занято сотрудников:");
            System.out.println("  - Механики: " + mechanicBusy);
            System.out.println("  - Электронщики: " + electronBusy);
            System.out.println("  - Технологи: " + techBusy);
            System.out.println("  - Электрики: " + elecBusy);
            System.out.println("  - Комплектация: " + conplectBusy);

            // Вычисляем количество свободных сотрудников
            int mechanicFree = (int) mechanicCount - mechanicBusy;
            int electronFree = (int) electronCount - electronBusy;
            int techFree = (int) techCount - techBusy;
            int elecFree = (int) elecCount - elecBusy;
            int conplectFree = (int) conplectCount - conplectBusy;
            
            System.out.println("[AnalisHeaderService] Свободно сотрудников:");
            System.out.println("  - Механики: " + mechanicFree);
            System.out.println("  - Электронщики: " + electronFree);
            System.out.println("  - Технологи: " + techFree);
            System.out.println("  - Электрики: " + elecFree);
            System.out.println("  - Комплектация: " + conplectFree);

            // Получаем количество транзакций со статусом "В работе"
            System.out.println("[AnalisHeaderService] Подсчет транзакций 'В работе'...");
            long transactionsStartTime = System.currentTimeMillis();
            
            long inProgressTransactionsCount = pppRepository.countByStatus("В работе");
            
            long transactionsTime = System.currentTimeMillis() - transactionsStartTime;
            System.out.println("[AnalisHeaderService] Транзакции подсчитаны за " + transactionsTime + " мс");
            System.out.println("[AnalisHeaderService] Транзакций 'В работе': " + inProgressTransactionsCount);

            // Создаем DTO и заполняем данными
            System.out.println("[AnalisHeaderService] Создание DTO...");
            AnalisHeaderDTO dto = new AnalisHeaderDTO();
            
            // Нормативы
            dto.setVhodNorm(VhodControl != null ? VhodControl.getNorm() : null);
            dto.setPodklyuchenieNorm(Podklyuchenie != null ? Podklyuchenie.getNorm() : null);
            dto.setMechOperationNorm(MechOperation != null ? MechOperation.getNorm() : null);
            dto.setElectronOperationNorm(ElectronOperation != null ? ElectronOperation.getNorm() : null);
            dto.setTechOperationNorm(TechOperation != null ? TechOperation.getNorm() : null);
            dto.setVihodNorm(Vihod != null ? Vihod.getNorm() : null);
            dto.setTransportNorm(Transport != null ? Transport.getNorm() : null);
            
            // Всего сотрудников
            dto.setMechanicCount((int) mechanicCount);
            dto.setEletronCount((int) electronCount);
            dto.setTechCount((int) techCount);
            dto.setElecCount((int) elecCount);
            dto.setConplectCount((int) conplectCount);
            
            // Занято сотрудников
            dto.setMechanicBusy(mechanicBusy);
            dto.setEletronBusy(electronBusy);
            dto.setTechBusy(techBusy);
            dto.setElecBusy(elecBusy);
            dto.setConplectBusy(conplectBusy);
            
            // Свободно сотрудников
            dto.setMechanicFree(mechanicFree);
            dto.setEletronFree(electronFree);
            dto.setTechFree(techFree);
            dto.setElecFree(elecFree);
            dto.setConplectFree(conplectFree);

            // Статистика транзакций
            dto.setInProgressTransactionsCount(inProgressTransactionsCount);
            
            // Общая сумма нормативов
            System.out.println("[AnalisHeaderService] Расчет общей суммы нормативов...");
            double total = 0.0;
            total += parseNorm(dto.getVhodNorm());
            total += parseNorm(dto.getPodklyuchenieNorm());
            total += parseNorm(dto.getMechOperationNorm());
            total += parseNorm(dto.getElectronOperationNorm());
            total += parseNorm(dto.getTechOperationNorm());
            total += parseNorm(dto.getVihodNorm());
            total += parseNorm(dto.getTransportNorm());
            dto.setTotalHeaderNorms(total);
            
            System.out.println("[AnalisHeaderService] Общая сумма нормативов: " + total);
            
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("[AnalisHeaderService] === ЗАВЕРШЕНИЕ getNorms() ===");
            System.out.println("[AnalisHeaderService] Общее время выполнения: " + totalTime + " мс");
            
            return dto;
            
        } catch (Exception e) {
            System.err.println("[AnalisHeaderService] ОШИБКА в getNorms(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Оптимизированный метод для подсчета занятых сотрудников из списка
     */
    private int countBusyEmployeesFromList(List<String> employees, String specialization) {
        System.out.println("[AnalisHeaderService] Подсчет занятых " + specialization + " (" + employees.size() + " чел)...");
        long startTime = System.currentTimeMillis();
        
        int busyCount = 0;
        int checkedCount = 0;
        
        for (String employeeName : employees) {
            checkedCount++;
            if (checkedCount % 10 == 0) {
                System.out.println("[AnalisHeaderService] Проверено " + checkedCount + "/" + employees.size() + " " + specialization);
            }
            
            // ПРЯМОЙ ЗАПРОС к базе данных для проверки занятости
            boolean isBusy = operationNewRepository.existsByOperationEmployeeAndStatusPppNotAndOperationStopWorkIsNull(
                employeeName, "Закрыта");
            
            if (isBusy) {
                busyCount++;
            }
        }
        
        long time = System.currentTimeMillis() - startTime;
        System.out.println("[AnalisHeaderService] " + specialization + ": " + busyCount + " занятых из " + employees.size() + " за " + time + " мс");
        
        return busyCount;
    }

    private double parseNorm(String normValue) {
        if (normValue == null || normValue.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(normValue);
        } catch (NumberFormatException e) {
            System.err.println("[AnalisHeaderService] Ошибка парсинга норматива: '" + normValue + "'");
            return 0.0;
        }
    }
    
    public double getTotalHeaderNorms() {
        System.out.println("[AnalisHeaderService] Вызов getTotalHeaderNorms()...");
        long startTime = System.currentTimeMillis();
        
        try {
            AnalisHeaderDTO dto = this.getNorms();
            double result = dto.getTotalHeaderNorms();
            
            long time = System.currentTimeMillis() - startTime;
            System.out.println("[AnalisHeaderService] getTotalHeaderNorms() выполнен за " + time + " мс, результат: " + result);
            
            return result;
        } catch (Exception e) {
            System.err.println("[AnalisHeaderService] ОШИБКА в getTotalHeaderNorms(): " + e.getMessage());
            throw e;
        }
    }
}