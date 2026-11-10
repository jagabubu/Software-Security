package com.northbridge.casevault;

import com.northbridge.casevault.exception.NotFoundException;
import com.northbridge.casevault.model.Category;
import com.northbridge.casevault.model.IncidentCase;
import com.northbridge.casevault.model.Severity;
import com.northbridge.casevault.service.AuditService;
import com.northbridge.casevault.service.IncidentCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CaseAccessSecurityTest {

    @Autowired
    private IncidentCaseService caseService;

    @Autowired
    private AuditService auditService;

    // TEST 1: a client can open their OWN organisation's case (the control test)
    @Test
    void clientCanViewOwnOrganisationCase() {
        List<IncidentCase> birchCases = caseService.listCases("birch_client1");
        assertFalse(birchCases.isEmpty());

        Long ownCaseId = birchCases.get(0).getId();
        IncidentCase found = caseService.getCase(ownCaseId, "birch_client1");

        assertEquals(ownCaseId, found.getId());
    }

    // TEST 2: the IDOR test. Acme tries to open a Birch case by its id.
    @Test
    void clientCannotViewAnotherOrganisationsCase() {
        Long birchCaseId = caseService.listCases("birch_client1").get(0).getId();

        assertThrows(NotFoundException.class,
                () -> caseService.getCase(birchCaseId, "acme_client1"));
    }

    // TEST 3: the list only contains the client's own organisation's cases
    @Test
    void caseListOnlyContainsOwnOrganisationCases() {
        List<IncidentCase> acmeCases = caseService.listCases("acme_client1");

        for (IncidentCase c : acmeCases) {
            assertEquals("Acme Ltd", c.getOrganisation().getName());
        }
    }

    // TEST 4: privilege escalation. An analyst must not be able to create a case.
    @Test
    void analystCannotCreateCase() {
        assertThrows(AccessDeniedException.class,
                () -> caseService.createCase("analyst1", "Title", "Description",
                        Category.OTHER, Severity.LOW));
    }

    // TEST 5: a denied attempt must leave an audit record
    @Test
    void deniedAttemptIsRecordedInAuditLog() {
        Long birchCaseId = caseService.listCases("birch_client1").get(0).getId();
        long before = auditService.countEvents();

        assertThrows(NotFoundException.class,
                () -> caseService.getCase(birchCaseId, "acme_client1"));

        assertEquals(before + 1, auditService.countEvents());
    }
}