package com.northbridge.casevault.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    public void assignTo(User analyst) {
        this.owner = analyst;
        if (this.status == Status.OPEN) {
            this.status = Status.IN_PROGRESS;
        }
    }

    // THE single place that decides which status changes are allowed
    public List<Status> allowedNextStatuses() {
        List<Status> result = new ArrayList<>();
        for (Status candidate : Status.values()) {
            if (!status.canMoveTo(candidate)) {
                continue;
            }
            if (status == Status.OPEN && owner == null) {
                continue;   // a case with no owner cannot be started
            }
            result.add(candidate);
        }
        return result;
    }

    public void changeStatus(Status next) {
        if (!allowedNextStatuses().contains(next)) {
            throw new IllegalStateException("Status change not allowed");
        }
        this.status = next;
    }
}