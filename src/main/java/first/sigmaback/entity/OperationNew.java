package first.sigmaback.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "operation_new")
public class OperationNew {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long operationId;

    @Column(name = "transaction")
    private String operationTransaction;

    @Column(name = "work_ppp")
    private String stagePpp;
    
    @Column(name = "start")
    private Timestamp operationStartWork; 

    @Column(name = "stop")
    private Timestamp operationStopWork;
    
    @Column(name = "employees")
    private String operationEmployee;

    // Новые поля, которых нет в старой таблице
    @Column(name = "stage_ppp")
    private String operationType;

    @Column(name = "status_work_ppp")
    private String statusWorkPpp;

    @Column(name = "status_ppp")
    private String statusPpp;
}