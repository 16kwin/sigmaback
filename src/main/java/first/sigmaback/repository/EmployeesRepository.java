package first.sigmaback.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import first.sigmaback.entity.Employees;

public interface EmployeesRepository  extends JpaRepository<Employees, Long>{

     Employees findFirstByEmployeesName(String employeesName);

}
