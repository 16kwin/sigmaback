package first.sigmaback.entity;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "operation_norm") // Замените на имя вашей таблицы в БД
@Data 
public class OperationNorm {
 @Id
    @Column(name = "name")
    private String normName;

    @Column(name = "norm")
    private String norm;

    @Column(name = "type")
    private String normType;

    @Column(name = "category")
    private String normCategory;

}
