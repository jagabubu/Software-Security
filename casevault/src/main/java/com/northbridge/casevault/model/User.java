package com.northbridge.casevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(optional = false)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @Column(nullable = false)
    private boolean enabled = true;

    protected User() { }

    public User(String username, String passwordHash, Role role, Organisation organisation) {
        this.username = username;
        this.passwordHash = passwordHash;      // BLANK 1
        this.role = role;                      // BLANK 2
        this.organisation = organisation;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public Organisation getOrganisation() { return organisation; }
    public boolean isEnabled() { return enabled; }

    public void disable() { this.enabled = false; }

    public boolean canAccessOrganisation(Long orgId) {
        return enabled && organisation.getId().equals(orgId);   // BLANK 3
    }
}