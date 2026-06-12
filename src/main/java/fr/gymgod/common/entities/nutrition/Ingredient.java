package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "ingredients")
@Data
public class Ingredient {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    //ingredients_text
    @Column(columnDefinition = "TEXT", unique = true)
    private String name;
}
