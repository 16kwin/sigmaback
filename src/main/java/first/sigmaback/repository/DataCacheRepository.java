package first.sigmaback.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import first.sigmaback.entity.DataCache;

import java.util.Optional;

@Repository
public interface DataCacheRepository extends JpaRepository<DataCache, Long> {
    Optional<DataCache> findByJsonName(String jsonName); // Метод для поиска по имени
}