package first.sigmaback.entity;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="time")
public class Time{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Column(name = "id")
    private Long timeId;

    @Column(name = "mounth")
    private LocalDate mounth;

    @Column(name = "hours")
    private Integer hoursMounth;

}