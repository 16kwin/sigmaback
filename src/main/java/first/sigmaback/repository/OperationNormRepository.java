package first.sigmaback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import first.sigmaback.entity.OperationNorm;
import java.util.List;

public interface OperationNormRepository extends JpaRepository<OperationNorm, Long> {
    List<OperationNorm> findByWorkPpp(String workPpp);
}