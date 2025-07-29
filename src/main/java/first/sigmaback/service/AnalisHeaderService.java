package first.sigmaback.service;

import first.sigmaback.dto.AnalisHeaderDTO;
import first.sigmaback.entity.OperationNorm;
import first.sigmaback.repository.OperationNormRepository;
import first.sigmaback.repository.EmployeesRepository;
import first.sigmaback.repository.PppRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalisHeaderService {

    private final OperationNormRepository operationNormRepository;
    private final EmployeesRepository employeesRepository;
    private final PppRepository pppRepository;

    public AnalisHeaderService(OperationNormRepository operationNormRepository, EmployeesRepository employeesRepository, PppRepository pppRepository) {
        this.operationNormRepository = operationNormRepository;
        this.employeesRepository = employeesRepository;
        this.pppRepository = pppRepository;
    }

    public AnalisHeaderDTO getNorms() {
        // Получаем нормативы для операций
        OperationNorm VhodControl = operationNormRepository.findById("Входной контроль").orElse(null);
        OperationNorm Podklyuchenie = operationNormRepository.findById("Подключение").orElse(null);
        OperationNorm MechOperation = operationNormRepository.findById("Проверка механиком").orElse(null);
        OperationNorm ElectronOperation = operationNormRepository.findById("Проверка электронщиком").orElse(null);
        OperationNorm TechOperation = operationNormRepository.findById("Проверка технологом").orElse(null);
        OperationNorm Vihod = operationNormRepository.findById("Выходной контроль").orElse(null);
        OperationNorm Transport = operationNormRepository.findById("Транспортное положение").orElse(null);

        // Получаем количество уникальных работников разных профессий
        long mechanicCount = employeesRepository.findDistinctEmployeesNameByEmployeesSpecializationIgnoreCase("Механик").size();
        long electronCount = employeesRepository.findDistinctEmployeesNameByEmployeesSpecializationIgnoreCase("Электронщик").size();
        long techCount = employeesRepository.findDistinctEmployeesNameByEmployeesSpecializationIgnoreCase("Технолог").size();
        long elecCount = employeesRepository.findDistinctEmployeesNameByEmployeesSpecializationIgnoreCase("Электрик").size();
        long conplectCount = employeesRepository.findDistinctEmployeesNameByEmployeesSpecializationIgnoreCase("Комплектация").size();

        // Получаем количество транзакций со статусом "В работе"
        long inProgressTransactionsCount = pppRepository.countByStatus("В работе");

        // Создаем DTO и заполняем данными
        AnalisHeaderDTO dto = new AnalisHeaderDTO();
        dto.setVhodNorm(VhodControl != null ? VhodControl.getNorm() : null);
        dto.setPodklyuchenieNorm(Podklyuchenie != null ? Podklyuchenie.getNorm() : null);
        dto.setMechOperationNorm(MechOperation != null ? MechOperation.getNorm() : null);
        dto.setElectronOperationNorm(ElectronOperation != null ? ElectronOperation.getNorm() : null);
        dto.setTechOperationNorm(TechOperation != null ? TechOperation.getNorm() : null);
        dto.setVihodNorm(Vihod != null ? Vihod.getNorm() : null);
        dto.setTransportNorm(Transport != null ? Transport.getNorm() : null);
        dto.setMechanicCount((int) mechanicCount);
        dto.setEletronCount((int) electronCount);
        dto.setTechCount((int) techCount);
        dto.setElecCount((int) elecCount);
        dto.setConplectCount((int) conplectCount);

        // Устанавливаем количество транзакций со статусом "В работе"
        dto.setInProgressTransactionsCount(inProgressTransactionsCount);
double total = 0.0;
    
    // Безопасное сложение всех нормативов
    total += parseNorm(dto.getVhodNorm());
    total += parseNorm(dto.getPodklyuchenieNorm());
    total += parseNorm(dto.getMechOperationNorm());
    total += parseNorm(dto.getElectronOperationNorm());
    total += parseNorm(dto.getTechOperationNorm());
    total += parseNorm(dto.getVihodNorm());
    total += parseNorm(dto.getTransportNorm());
    
    dto.setTotalHeaderNorms(total);
        return dto;
    }

private double parseNorm(String normValue) {
    if (normValue == null || normValue.trim().isEmpty()) {
        return 0.0;
    }
    try {
        return Double.parseDouble(normValue);
    } catch (NumberFormatException e) {
        return 0.0;
    }
}
public double getTotalHeaderNorms() {
    AnalisHeaderDTO dto = this.getNorms(); // получаем DTO с уже рассчитанными нормами
    return dto.getTotalHeaderNorms(); // возвращаем рассчитанную сумму
}
}