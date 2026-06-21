package first.sigmaback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import first.sigmaback.entity.OperationNorm;
import java.util.Optional;

public interface OperationNormRepository extends JpaRepository<OperationNorm, Long> {
    Optional<OperationNorm> findByWorkPpp(String workPpp);
}