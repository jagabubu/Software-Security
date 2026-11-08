package com.northbridge.casevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "organisations")
public class Organisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    protected Organisation() { }

    public Organisation(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
}