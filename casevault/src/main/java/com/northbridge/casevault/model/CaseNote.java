package com.northbridge.casevault.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "case_notes")
public class CaseNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "case_id")
    private IncidentCase incidentCase;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected CaseNote() { }

    public CaseNote(IncidentCase incidentCase, User author, String content) {
        this.incidentCase = incidentCase;
        this.author = author;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public IncidentCase getIncidentCase() { return incidentCase; }
    public User getAuthor() { return author; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}