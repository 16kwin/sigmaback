package first.sigmaback.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import first.sigmaback.entity.Problems;

public interface ProblemsRepository extends JpaRepository<Problems, Long> {
}
