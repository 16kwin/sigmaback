package first.sigmaback.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "operation_norms")
public class OperationNorm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "machine_type")
    private String machineType;

    @Column(name = "work_ppp")
    private String workPpp;

    @Column(name = "specialty")
    private String specialty;

    @Column(name = "operation_norm")
    private BigDecimal operationNorm;

    @Column(name = "operation_option_ppp")
    private String operationOptionPpp;
}