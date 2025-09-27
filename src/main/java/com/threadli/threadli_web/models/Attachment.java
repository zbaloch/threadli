package com.threadli.threadli_web.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String fileName;

    private String fileType;

    // Store binary data directly as a byte[] without @Lob so Hibernate will
    // use getBytes() instead of getBlob(). The SQLite JDBC driver does not
    // implement ResultSet#getBlob(), which previously caused
    // SQLFeatureNotSupportedException when this field was annotated with @Lob.
    // Using a MySQL-specific columnDefinition (LONGBLOB) also confused the
    // dialect. A plain BLOB affinity column is fine in SQLite.
    @Column(name = "data")
    private byte[] data;

    @ManyToOne
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

}