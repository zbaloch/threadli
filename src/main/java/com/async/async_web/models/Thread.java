package com.async.async_web.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Thread {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private boolean isPinned = false;

    @ManyToOne
	@JoinColumn(name = "workspace_id")
	private Workspace workspace; 

    // Thread can only be cloased by who created it
    private boolean isClosed = false;
    
    // TODO: Add conclusion later
    // @Lob
    // @Column(columnDefinition = "TEXT")
    // private String conclusion = null;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    // @PreUpdate
    // protected void onUpdate() {
    //     updatedAt = Instant.now();
    // }

}
