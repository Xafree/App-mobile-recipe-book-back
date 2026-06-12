package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "traces")
@Data
public class Trace {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    //traces
    @Column(columnDefinition = "TEXT", unique = true)
    private String name;
}
