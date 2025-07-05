package first.sigmaback.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import first.sigmaback.entity.Ppp;

public interface PppRepository  extends JpaRepository<Ppp, String>{
    
}
