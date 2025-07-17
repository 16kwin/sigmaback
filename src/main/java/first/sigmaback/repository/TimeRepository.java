package first.sigmaback.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import first.sigmaback.entity.Time;

public interface TimeRepository extends JpaRepository<Time, Long> {

    Optional<Time> findByMounth(LocalDate mounth);
}