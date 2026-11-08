package com.northbridge.casevault.config;

import com.northbridge.casevault.model.Organisation;
import com.northbridge.casevault.model.Role;
import com.northbridge.casevault.model.User;
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
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;

    public DevDataLoader(OrganisationRepository organisationRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         @Value("${casevault.seed-password}") String seedPassword) {
        this.organisationRepository = organisationRepository;
        this.userRepository = userRepository;
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

        addUser("acme_client1", Role.CLIENT, acme);
        addUser("birch_client1", Role.CLIENT, birch);
        addUser("analyst1", Role.ANALYST, northbridge);
        addUser("manager1", Role.CASE_MANAGER, northbridge);
        addUser("auditor1", Role.AUDITOR_ADMIN, northbridge);

        System.out.println("Seeded users: " + userRepository.count());
    }

    private void addUser(String username, Role role, Organisation organisation) {
        String hash = passwordEncoder.encode(seedPassword);
        userRepository.save(new User(username, hash, role, organisation));
    }
}