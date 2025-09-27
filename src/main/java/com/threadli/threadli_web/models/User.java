package com.threadli.threadli_web.models;

import java.time.Instant;
import java.util.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String password;

    private String firstName;

    private String lastName;

    private String token;
    private Calendar tokenExpirationDate;
    private Calendar tokenUsedDate;

    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "user")
    private Set<WorkspaceMembership> workspaceMemberships = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<ThreadMembership> threads = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    // @PrePersist
    // protected void onUpdate() {
    //     updatedAt = Instant.now();
    // }
    
}
