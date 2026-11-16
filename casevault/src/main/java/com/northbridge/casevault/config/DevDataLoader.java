package com.northbridge.casevault.config;

import com.northbridge.casevault.model.Category;
import com.northbridge.casevault.model.IncidentCase;
import com.northbridge.casevault.model.Organisation;
import com.northbridge.casevault.model.Role;
import com.northbridge.casevault.model.Severity;
import com.northbridge.casevault.model.User;
import com.northbridge.casevault.repository.IncidentCaseRepository;
import com.northbridge.casevault.repository.OrganisationRepository;
import com.northbridge.casevault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DevDataLoader implements CommandLineRunner {

    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final IncidentCaseRepository incidentCaseRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;

    public DevDataLoader(OrganisationRepository organisationRepository,
                         UserRepository userRepository,
                         IncidentCaseRepository incidentCaseRepository,
                         PasswordEncoder passwordEncoder,
                         @Value("${casevault.seed-password}") String seedPassword) {
        this.organisationRepository = organisationRepository;
        this.userRepository = userRepository;
        this.incidentCaseRepository = incidentCaseRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        Organisation acme = organisationRepository.save(new Organisation("Acme Ltd"));
        Organisation birch = organisationRepository.save(new Organisation("Birch Co"));
        Organisation northbridge = organisationRepository.save(new Organisation("Northbridge"));

        User acmeClient = addUser("acme_client1", Role.CLIENT, acme);
        User birchClient = addUser("birch_client1", Role.CLIENT, birch);
        addUser("analyst1", Role.ANALYST, northbridge);
        addUser("analyst2", Role.ANALYST, northbridge);
        addUser("temp_analyst", Role.ANALYST, northbridge);
        addUser("manager1", Role.CASE_MANAGER, northbridge);
        addUser("auditor1", Role.AUDITOR_ADMIN, northbridge);

        incidentCaseRepository.save(new IncidentCase(
                "Phishing email to finance team",
                "Synthetic test case: staff received a suspicious invoice email.",
                Category.PHISHING, Severity.HIGH, acme, acmeClient));
        incidentCaseRepository.save(new IncidentCase(
                "Ransomware note on file server",
                "Synthetic test case: a ransom note appeared on a shared drive.",
                Category.RANSOMWARE, Severity.CRITICAL, acme, acmeClient));
        incidentCaseRepository.save(new IncidentCase(
                "Lost laptop report",
                "Synthetic test case: an employee laptop was left on a train.",
                Category.OTHER, Severity.MEDIUM, birch, birchClient));

        System.out.println("Seeded users: " + userRepository.count());
    }

    private User addUser(String username, Role role, Organisation organisation) {
        String hash = passwordEncoder.encode(seedPassword);
        return userRepository.save(new User(username, hash, role, organisation));
    }
}