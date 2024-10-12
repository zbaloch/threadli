package com.threadli.threadli_web.models;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "thread_members")
@Getter
@Setter
public class ThreadMembership {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "thread_id")
    private Thread thread;

    private Boolean caughtUp;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public ThreadMembership(User user, Thread thread) {
        super();
        this.user = user;
        this.thread = thread;
    }

    public ThreadMembership() {
        super();
    }
}
