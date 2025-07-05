package first.sigmaback.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import first.sigmaback.entity.Operation;

public interface OperationRepository  extends JpaRepository<Operation, Long>{
 List<Operation> findByOperationTransaction(String operationTransaction);
}
