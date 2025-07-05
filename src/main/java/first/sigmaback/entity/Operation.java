package first.sigmaback.entity;

import java.sql.Timestamp;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="operation")
public class Operation{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Column(name = "id")
    private Long operationId;

    @Column(name = "transaction")
    private String operationTransaction;

    @Column(name = "type")
    private String operationType;
    
    @Column(name = "start")
    private Timestamp operationStartWork; 

    @Column(name = "stop")
    private Timestamp operationStopWork;
     @Column(name = "employee")
    private String operationEmployee;
}
