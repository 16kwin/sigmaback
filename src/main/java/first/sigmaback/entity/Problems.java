package first.sigmaback.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="problems")
public class Problems{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Column(name = "id")
    private Long problemsId;

    @Column(name = "transaction")
    private String problemsTransaction;

    @Column(name = "type")
    private String problemsType;

    @Column(name = "description")
    private String problemsDescription;
    
    @Column(name = "norm_hours")
    private Double problemsHours; 

     @Column(name = "employee")
    private String problemsEmployee;
}
