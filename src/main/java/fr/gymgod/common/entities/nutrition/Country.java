package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "country")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Country {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(columnDefinition = "TEXT", unique = true)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "country_variants", joinColumns = @JoinColumn(name = "country_id"))
    @Column(name = "variant")
    private List<String> variants = new ArrayList<>();
}
