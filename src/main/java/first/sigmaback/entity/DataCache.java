package first.sigmaback.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="jsons")
public class DataCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Column(name = "id")
    private Long jsonId;

    @Column(name = "name")
    private String jsonName;

    @Column(name = "json")
    private String json;
    @Column(name = "time")
    private String jsonTime;
}
