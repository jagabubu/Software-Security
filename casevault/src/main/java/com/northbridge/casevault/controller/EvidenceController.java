package com.northbridge.casevault.controller;

import com.northbridge.casevault.exception.EvidenceRejectedException;
import com.northbridge.casevault.service.EvidenceDownload;
import com.northbridge.casevault.service.EvidenceService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @GetMapping("/cases/{id}/evidence")
    public String list(@PathVariable Long id, Authentication authentication, Model model) {
        String username = authentication.getName();
        model.addAttribute("caseId", id);
        model.addAttribute("files", evidenceService.listEvidence(username, id));
        model.addAttribute("canUpload", evidenceService.canUpload(username, id));
        return "evidence";
    }

    @PostMapping("/cases/{id}/evidence")
    public String upload(@PathVariable Long id,
                         @RequestParam("file") MultipartFile file,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            evidenceService.upload(authentication.getName(), id, file);
            redirectAttributes.addFlashAttribute("message", "Evidence uploaded.");
        } catch (EvidenceRejectedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cases/" + id + "/evidence";
    }

    @GetMapping("/cases/{caseId}/evidence/{evidenceId}")
    public ResponseEntity<Resource> download(@PathVariable Long caseId,
                                             @PathVariable Long evidenceId,
                                             Authentication authentication) {
        EvidenceDownload d = evidenceService.download(authentication.getName(), caseId, evidenceId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.filename()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(d.path()));
    }
}