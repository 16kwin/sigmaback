package first.sigmaback.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import first.sigmaback.entity.Problems;

public interface ProblemsRepository extends JpaRepository<Problems, Long> {

    List<Problems> findByProblemsTransaction(String transactionId);
}
