package com.northbridge.casevault.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cases")
public class IncidentCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected IncidentCase() { }

    public IncidentCase(String title, String description, Category category,
                        Severity severity, Organisation organisation, User createdBy) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.severity = severity;
        this.status = Status.OPEN;
        this.organisation = organisation;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public Severity getSeverity() { return severity; }
    public Status getStatus() { return status; }
    public Organisation getOrganisation() { return organisation; }
    public User getOwner() { return owner; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}