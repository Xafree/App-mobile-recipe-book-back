package fr.gymgod.common.entities.nutrition;

import fr.gymgod.common.entities.nutrition.Country;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

// Index pg_trgm à créer MANUELLEMENT une fois en base (ddl-auto=update ne gère
// pas les index GIN). Se connecter à gymgod_nutrition et exécuter :
//
//   CREATE EXTENSION IF NOT EXISTS pg_trgm;
//   CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_name_trgm
//       ON products USING GIN (product_name gin_trgm_ops);
//
// Cet index rend les ILIKE '%%' et ILIKE '%texte%' sur 656 k lignes
// instantanés (~2 ms vs ~800 ms avec un full scan ou B-tree inutilisable).
// L'index B-tree idx_product_name ci-dessous reste utile pour les lookups
// exacts (=) — les deux coexistent sans conflit.
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_name", columnList = "productName")
})
@Data
public class Product {

    // code
    @Id
    @Column(length = 50)
    private String code;

    // url
    @Column(columnDefinition = "TEXT")
    private String url;

    // created_t
    @Column
    private String createdTime;

    @Column
    private String version;

    // last_modified_t
    @Column
    private String lastModifiedTime;

    // product_name
    @Column(columnDefinition = "TEXT")
    private String productName;

    // quantity
    @Column(columnDefinition = "TEXT")
    private String quantity;

    @Column(columnDefinition = "TEXT")
    private String ingredientsText;

    @ManyToOne(cascade = CascadeType.ALL)
    private Brand brand;

    @ManyToMany(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    private List<Categorie> categorie;

    @ManyToMany(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    private List<Country> country;

    @ManyToMany(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    private List<Label> label;

    @ManyToMany(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    private List<Ingredient> ingredient;

    @ManyToMany(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    private List<Allergen> allergen;

    @ManyToMany(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    private List<Trace> trace;

    @ManyToMany(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    private List<Additive> additives;

    // nutriscore_score
    private int nutriscore;

    @OneToOne(cascade = CascadeType.ALL)
    private Nutriment nutriment;

    @OneToOne(cascade = CascadeType.ALL)
    private Glucide glucide;

    @OneToOne(cascade = CascadeType.ALL)
    private Vitamin vitamin;

    @OneToOne(cascade = CascadeType.ALL)
    private Images image;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean aiEnriched = false;

    @Column(columnDefinition = "TEXT")
    private String aiError;
}
