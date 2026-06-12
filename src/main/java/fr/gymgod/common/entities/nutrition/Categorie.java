package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "categories")
@Data
public class Categorie {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    //categories
    @Column(columnDefinition = "TEXT", unique = true)
    private String name;

}
