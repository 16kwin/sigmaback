package first.sigmaback.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="employees")
public class Employees {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Column(name = "id")
    private Long employeesId;

    @Column(name = "name")
    private String employeesName;

    @Column(name = "specialization")
    private String employeesSpecialization;
}
