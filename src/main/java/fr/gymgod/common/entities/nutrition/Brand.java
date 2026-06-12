package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "brands")
@Data
public class Brand {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    //brands
    @Column(columnDefinition = "TEXT", unique = true)
    private String name;
}
