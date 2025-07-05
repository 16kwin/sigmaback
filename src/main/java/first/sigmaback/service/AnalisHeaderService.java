package first.sigmaback.service;

import first.sigmaback.dto.AnalisHeaderDTO;
import first.sigmaback.entity.OperationNorm;
import first.sigmaback.repository.OperationNormRepository;
import first.sigmaback.repository.EmployeesRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalisHeaderService {

    private final OperationNormRepository operationNormRepository;
    private final EmployeesRepository employeesRepository;

    public AnalisHeaderService(OperationNormRepository operationNormRepository, EmployeesRepository employeesRepository) {
        this.operationNormRepository = operationNormRepository;
        this.employeesRepository = employeesRepository;
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

        // Получаем количество работников разных профессий
        long mechanicCount = employeesRepository.findAll().stream()
                .filter(employee -> employee.getEmployeesSpecialization().equals("Механик"))
                .count();
        long electronCount = employeesRepository.findAll().stream()
                .filter(employee -> employee.getEmployeesSpecialization().equals("Электронщик"))
                .count();
        long techCount = employeesRepository.findAll().stream()
                .filter(employee -> employee.getEmployeesSpecialization().equals("Технолог"))
                .count();
        long elecCount = employeesRepository.findAll().stream()
                .filter(employee -> employee.getEmployeesSpecialization().equals("Электрик"))
                .count();
        long conplectCount = employeesRepository.findAll().stream()
                .filter(employee -> employee.getEmployeesSpecialization().equals("Комплектация"))
                .count();


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

        return dto;
    }
}