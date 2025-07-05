package first.sigmaback.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import first.sigmaback.entity.OperationNorm;

public interface OperationNormRepository extends JpaRepository<OperationNorm, String> {

    OperationNorm findByNormName(String operationType);
}