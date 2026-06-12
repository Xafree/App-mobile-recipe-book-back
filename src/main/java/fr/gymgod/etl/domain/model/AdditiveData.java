package fr.gymgod.etl.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdditiveData {
    private String name;
    private String code;
    private int dangerLevel;
}
