package com.northbridge.casevault.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant eventTime;

    @Column(updatable = false, length = 100)
    private String username;

    @Column(nullable = false, updatable = false, length = 50)
    private String action;

    @Column(updatable = false, length = 100)
    private String target;

    @Column(nullable = false, updatable = false, length = 20)
    private String outcome;

    protected AuditEvent() { }

    public AuditEvent(String username, String action, String target, String outcome) {
        this.eventTime = Instant.now();
        this.username = username;
        this.action = action;
        this.target = target;
        this.outcome = outcome;
    }

    public Long getId() { return id; }
    public Instant getEventTime() { return eventTime; }
    public String getUsername() { return username; }
    public String getAction() { return action; }
    public String getTarget() { return target; }
    public String getOutcome() { return outcome; }
}