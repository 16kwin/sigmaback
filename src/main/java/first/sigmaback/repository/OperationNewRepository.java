package first.sigmaback.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import first.sigmaback.entity.OperationNew;

public interface OperationNewRepository extends JpaRepository<OperationNew, Long> {
    List<OperationNew> findByOperationTransaction(String operationTransaction);

    long countByOperationTransaction(String transactionId);
    
    List<OperationNew> findByStagePpp(String stagePpp);
    
    // Метод для поиска операций по сотруднику
    List<OperationNew> findByOperationEmployee(String operationEmployee);
    
    // Оптимизированный метод для проверки занятости сотрудника
    @Query("SELECT COUNT(o) > 0 FROM OperationNew o WHERE " +
           "o.operationEmployee = :employeeName AND " +
           "o.statusPpp != :closedStatus AND " +
           "o.operationStopWork IS NULL")
    boolean existsByOperationEmployeeAndStatusPppNotAndOperationStopWorkIsNull(
        @Param("employeeName") String employeeName,
        @Param("closedStatus") String closedStatus);
}