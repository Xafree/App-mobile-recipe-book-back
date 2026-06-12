package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "labels")
@Data
public class Label {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    //labels
    @Column(columnDefinition = "TEXT", unique = true)
    private String name;
}
