package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "additives", indexes = {
    @Index(name = "idx_additive_code", columnList = "code", unique = true)
})
@Data
public class Additive {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(unique = true)
    private String code; // e.g., "E330"

    @Column(columnDefinition = "TEXT")
    private String name; // e.g., "Acide citrique"

    @Column
    private int dangerLevel; // 0: Safe, 1: Moderate, 2: Risky, 3: Dangerous

    @Column(columnDefinition = "TEXT")
    private String description;
}
