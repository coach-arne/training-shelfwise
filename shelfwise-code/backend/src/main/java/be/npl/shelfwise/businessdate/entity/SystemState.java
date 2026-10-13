package be.npl.shelfwise.businessdate.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Single row table: the business date and the last date for which daily processing ran. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class SystemState {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id = SINGLETON_ID;

    private LocalDate businessDate;

    private LocalDate lastProcessedDate;
}
