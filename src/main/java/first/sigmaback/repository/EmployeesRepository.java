package first.sigmaback.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import first.sigmaback.entity.Employees;

public interface EmployeesRepository  extends JpaRepository<Employees, Long>{

     Employees findFirstByEmployeesName(String employeesName);
 @Query("SELECT DISTINCT e.employeesName FROM Employees e WHERE LOWER(e.employeesSpecialization) = LOWER(:specialization)")
List<String> findDistinctEmployeesNameByEmployeesSpecializationIgnoreCase(@Param("specialization") String specialization);
}
