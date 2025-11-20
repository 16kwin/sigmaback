package first.sigmaback.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import first.sigmaback.entity.OperationNew;

public interface OperationNewRepository extends JpaRepository<OperationNew, Long>{
    List<OperationNew> findByOperationTransaction(String operationTransaction);

    long countByOperationTransaction(String transactionId);
    List<OperationNew> findByStagePpp(String stagePpp);
}