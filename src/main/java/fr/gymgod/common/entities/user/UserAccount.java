package fr.gymgod.common.entities.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class UserAccount implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String username;

    @JsonIgnore
    private String password;

    @Column(unique = true)
    private String email;

    private String role;

    private boolean active = false;

    private String verificationCode;
    private LocalDateTime verificationCodeExpiresAt;
    private boolean emailVerified = false;

    // ── Champs enrichis (fusion app_recipe_book_back) ──────────────────
    private String firstName;
    private String lastName;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "apple_id", unique = true)
    private String appleId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private AuthProvider provider = AuthProvider.EMAIL;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    /** Courte biographie affichée sur le profil public. */
    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "calorie_goal")
    private Integer calorieGoal = 2000;

    @Column(name = "notifications_enabled")
    private Boolean notificationsEnabled = true;

    @Column(length = 2)
    private String language = "fr";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
