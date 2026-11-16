package com.northbridge.casevault.service;

import com.northbridge.casevault.exception.EvidenceRejectedException;
import com.northbridge.casevault.exception.NotFoundException;
import com.northbridge.casevault.model.EvidenceFile;
import com.northbridge.casevault.model.EvidenceType;
import com.northbridge.casevault.model.IncidentCase;
import com.northbridge.casevault.model.Role;
import com.northbridge.casevault.model.User;
import com.northbridge.casevault.repository.EvidenceFileRepository;
import com.northbridge.casevault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class EvidenceService {

    private static final long MAX_FILE_BYTES = 2L * 1024 * 1024;
    private static final long MAX_FILES_PER_CASE = 10;
    private static final int MAX_NAME_LENGTH = 100;

    private final IncidentCaseService caseService;
    private final UserRepository userRepository;
    private final EvidenceFileRepository evidenceRepository;
    private final AuditService auditService;
    private final Path baseDir;

    public EvidenceService(IncidentCaseService caseService,
                           UserRepository userRepository,
                           EvidenceFileRepository evidenceRepository,
                           AuditService auditService,
                           @Value("${casevault.evidence-dir}") String evidenceDir) {
        this.caseService = caseService;
        this.userRepository = userRepository;
        this.evidenceRepository = evidenceRepository;
        this.auditService = auditService;
        this.baseDir = Paths.get(evidenceDir).toAbsolutePath().normalize();
    }

    public List<EvidenceFile> listEvidence(String username, Long caseId) {
        requireVisible(username, caseId, "LIST_EVIDENCE");
        return evidenceRepository.findAllByIncidentCaseIdOrderByCreatedAtAsc(caseId);
    }

    // Only decides whether to SHOW the upload form. upload() enforces the rule itself.
    public boolean canUpload(String username, Long caseId) {
        Role role = loadUser(username).getRole();
        return (role == Role.CLIENT || role == Role.ANALYST)
                && caseService.findVisibleCase(username, caseId).isPresent();
    }

    public void upload(String username, Long caseId, MultipartFile file) {
        String target = "case:" + caseId;
        User user = loadUser(username);

        // 1. Who may touch this case at all? (same rule as everywhere else)
        Optional<IncidentCase> visible = caseService.findVisibleCase(username, caseId);
        if (visible.isEmpty()) {
            auditService.record(username, "UPLOAD_EVIDENCE", target, "DENIED");
            throw new NotFoundException("Case not found");
        }
        // 2. Is this role allowed to upload? (managers can download but not upload)
        if (user.getRole() != Role.CLIENT && user.getRole() != Role.ANALYST) {
            auditService.record(username, "UPLOAD_EVIDENCE", target, "DENIED");
            throw new AccessDeniedException("Not allowed");
        }

        // 3. Size, count, type and content checks
        if (file == null || file.isEmpty()) {
            throw reject(username, target, "Choose a file to upload.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw reject(username, target, "File is too large. The limit is 2 MB.");
        }
        if (evidenceRepository.countByIncidentCaseId(caseId) >= MAX_FILES_PER_CASE) {
            throw reject(username, target, "This case already has the maximum number of files.");
        }

        String safeName = sanitiseName(file.getOriginalFilename());
        EvidenceType type = EvidenceType.fromExtension(extensionOf(safeName))
                .orElseThrow(() -> reject(username, target,
                        "File type not allowed. Allowed types: txt, log, csv, pdf, png."));

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw reject(username, target, "The upload failed. Please try again.");
        }
        if (!FileSignatureChecker.matches(type, bytes)) {
            throw reject(username, target, "The file content does not match its type.");
        }

        // 4. Store under a RANDOM name. The user's filename is never used on disk.
        String storedName = UUID.randomUUID().toString();
        try {
            Files.createDirectories(baseDir);
            Files.write(resolveInside(storedName), bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw reject(username, target, "The upload failed. Please try again.");
        }

        evidenceRepository.save(new EvidenceFile(
                visible.get(), user, safeName, storedName, type, bytes.length, sha256(bytes)));
        auditService.record(username, "UPLOAD_EVIDENCE", target, "SUCCESS");
    }

    public EvidenceDownload download(String username, Long caseId, Long evidenceId) {
        requireVisible(username, caseId, "DOWNLOAD_EVIDENCE");
        String target = "evidence:" + evidenceId;

        // Looked up by evidence id AND case id, so an id from another case finds nothing
        EvidenceFile file = evidenceRepository.findByIdAndIncidentCaseId(evidenceId, caseId)
                .orElseThrow(() -> {
                    auditService.record(username, "DOWNLOAD_EVIDENCE", target, "DENIED");
                    return new NotFoundException("Not found");
                });

        Path path = resolveInside(file.getStoredName());
        if (!Files.isRegularFile(path)) {
            throw new NotFoundException("Not found");
        }

        auditService.record(username, "DOWNLOAD_EVIDENCE", target, "SUCCESS");
        return new EvidenceDownload(file.getOriginalName(), file.getFileType().getContentType(), path);
    }

    private IncidentCase requireVisible(String username, Long caseId, String action) {
        Optional<IncidentCase> visible = caseService.findVisibleCase(username, caseId);
        if (visible.isEmpty()) {
            auditService.record(username, action, "case:" + caseId, "DENIED");
            throw new NotFoundException("Case not found");
        }
        return visible.get();
    }

    // Records the rejection in the audit log and returns an exception with a SAFE message
    private EvidenceRejectedException reject(String username, String target, String message) {
        auditService.record(username, "UPLOAD_EVIDENCE", target, "REJECTED");
        return new EvidenceRejectedException(message);
    }

    // Display name only: strip any folder path, keep safe characters, cap the length
    private String sanitiseName(String original) {
        String name = (original == null) ? "" : original;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[^A-Za-z0-9._ -]", "_").replaceFirst("^\\.+", "");
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(name.length() - MAX_NAME_LENGTH);   // keep the end so the extension survives
        }
        return name.isBlank() ? "file" : name;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    // Defence in depth: even our own generated name must stay inside the storage folder
    private Path resolveInside(String storedName) {
        Path path = baseDir.resolve(storedName).normalize();
        if (!path.startsWith(baseDir)) {
            throw new IllegalStateException("Invalid storage path");
        }
        return path;
    }

    private String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Not found"));
    }
}