package com.northbridge.casevault.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "evidence_files")
public class EvidenceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "case_id")
    private IncidentCase incidentCase;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uploader_id")
    private User uploader;

    @Column(nullable = false, length = 100)
    private String originalName;

    @Column(nullable = false, unique = true, updatable = false, length = 60)
    private String storedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceType fileType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected EvidenceFile() { }

    public EvidenceFile(IncidentCase incidentCase, User uploader, String originalName,
                        String storedName, EvidenceType fileType, long sizeBytes, String sha256) {
        this.incidentCase = incidentCase;
        this.uploader = uploader;
        this.originalName = originalName;
        this.storedName = storedName;
        this.fileType = fileType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public IncidentCase getIncidentCase() { return incidentCase; }
    public User getUploader() { return uploader; }
    public String getOriginalName() { return originalName; }
    public String getStoredName() { return storedName; }
    public EvidenceType getFileType() { return fileType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public Instant getCreatedAt() { return createdAt; }
}