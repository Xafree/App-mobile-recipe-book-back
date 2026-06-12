package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "allergens")
@Data
public class Allergen {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    //allergens
    @Column(columnDefinition = "TEXT", unique = true)
    private String name;
}
