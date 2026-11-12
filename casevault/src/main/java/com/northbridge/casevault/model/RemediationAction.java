package com.northbridge.casevault.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "remediation_actions")
public class RemediationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "case_id")
    private IncidentCase incidentCase;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected RemediationAction() { }

    public RemediationAction(IncidentCase incidentCase, User author, String description) {
        this.incidentCase = incidentCase;
        this.author = author;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public IncidentCase getIncidentCase() { return incidentCase; }
    public User getAuthor() { return author; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}