package com.hauly.platform.auth.infrastructure.bootstrap;

import com.hauly.platform.auth.application.AuthService;
import com.hauly.platform.auth.application.command.BootstrapUserCommand;
import com.hauly.platform.auth.domain.model.AppUser;
import com.hauly.platform.auth.domain.model.Role;
import com.hauly.platform.auth.domain.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Bootstrap initial users on first startup.
 * Activates only when HAULY_BOOTSTRAP=true AND app_user table is empty.
 * Reads user credentials from environment variables (fail-fast if missing).
 */
@Component
public class InitialUserBootstrapper implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialUserBootstrapper.class);

    private final AppUserRepository userRepository;
    private final AuthService authService;

    public InitialUserBootstrapper(AppUserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        String bootstrapEnabled = System.getenv("HAULY_BOOTSTRAP");
        if (!"true".equalsIgnoreCase(bootstrapEnabled)) {
            log.info("Bootstrap disabled. Set HAULY_BOOTSTRAP=true to seed initial users.");
            return;
        }

        if (userRepository.count() > 0) {
            log.warn("Bootstrap skipped: app_user table already has users. " +
                     "Disable HAULY_BOOTSTRAP after first run.");
            return;
        }

        log.info("Bootstrap enabled — creating initial users...");

        String intakeEmail    = requireEnv("HAULY_BOOTSTRAP_INTAKE_EMAIL");
        String intakePassword = requireEnv("HAULY_BOOTSTRAP_INTAKE_PASSWORD");
        String buyerEmail     = requireEnv("HAULY_BOOTSTRAP_BUYER_EMAIL");
        String buyerPassword  = requireEnv("HAULY_BOOTSTRAP_BUYER_PASSWORD");

        AppUser intakeUser = authService.bootstrapUser(
                new BootstrapUserCommand(intakeEmail, intakePassword, Role.INTAKE, "Intake User"));
        log.info("Created user: email={}, role={}", intakeUser.getEmailValue(), intakeUser.getRole());

        AppUser buyerUser = authService.bootstrapUser(
                new BootstrapUserCommand(buyerEmail, buyerPassword, Role.BUYER, "Buyer User"));
        log.info("Created user: email={}, role={}", buyerUser.getEmailValue(), buyerUser.getRole());

        log.warn("Bootstrap complete. DISABLE HAULY_BOOTSTRAP after verifying users were created correctly.");
    }

    private String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Bootstrap env var '" + name + "' is required when HAULY_BOOTSTRAP=true but was not set.");
        }
        return value;
    }
}
