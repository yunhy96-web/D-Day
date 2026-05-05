package com.hauly.platform.auth.infrastructure.bootstrap;

import com.hauly.platform.auth.application.AuthService;
import com.hauly.platform.auth.application.command.BootstrapUserCommand;
import com.hauly.platform.auth.domain.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Bootstraps the two operator accounts (selim, union) on startup.
 *
 * Idempotent: a user that already exists is skipped, so this runner is safe to leave
 * enabled across restarts. New users get a random 24-char password that is appended to
 * a sealed secrets file ({@code hauly.bootstrap.passwords-file}, default
 * {@code /opt/hauly/secrets/new-users.txt}) which the operator reads once over SSH and
 * then deletes. If the file cannot be written we fall back to a WARN-level log line so
 * the credentials are never silently lost.
 */
@Component
public class InitialUserBootstrapper implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialUserBootstrapper.class);

    /** Spec for users that should always exist. Add here, never remove. */
    private static final List<UserSpec> SPECS = List.of(
            new UserSpec("selim", Role.ADMIN,  "Selim",   null),
            new UserSpec("union", Role.ADMIN,  "Union",   null),
            // 방문자용 조회 전용 계정 — 고정 비밀번호로 공유 가능.
            new UserSpec("user",  Role.VIEWER, "Visitor", "1234")
    );

    private final AuthService authService;
    private final Path passwordsFile;
    private final SecureRandom random = new SecureRandom();

    public InitialUserBootstrapper(
            AuthService authService,
            @Value("${hauly.bootstrap.passwords-file:/opt/hauly/secrets/new-users.txt}") String passwordsFilePath) {
        this.authService = authService;
        this.passwordsFile = Path.of(passwordsFilePath);
    }

    @Override
    public void run(String... args) {
        for (UserSpec spec : SPECS) {
            try {
                ensureUser(spec);
            } catch (Exception ex) {
                // Don't kill the boot — one bad spec shouldn't block the whole app.
                log.error("Failed to bootstrap user '{}': {}", spec.username(), ex.getMessage(), ex);
            }
        }
    }

    private void ensureUser(UserSpec spec) {
        // Generate a candidate password up-front so the find-or-create call is a single
        // transaction; ensureUser() drops the candidate if the user already existed.
        // fixedPassword가 지정된 spec(예: 공유용 viewer)은 랜덤 대신 그것을 사용.
        String rawPassword = spec.fixedPassword() != null ? spec.fixedPassword() : generatePassword();
        AuthService.BootstrapResult result = authService.ensureUser(new BootstrapUserCommand(
                spec.username(), rawPassword, spec.role(), spec.displayName()));

        if (!result.created()) {
            log.debug("Bootstrap: user '{}' already exists, skipping.", spec.username());
            return;
        }

        log.info("Bootstrap: created user id={} username={} role={}",
                result.user().getId(), result.user().getUsernameValue(), result.user().getRole());
        // fixedPassword는 코드에 이미 노출되어 있으므로 secrets file에 기록하지 않음.
        if (spec.fixedPassword() == null) {
            recordPassword(spec.username(), rawPassword);
        }
    }

    /** Generates a 24-char URL-safe random password (≈144 bits of entropy). */
    private String generatePassword() {
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Append (username, password) to the sealed secrets file. Creates parent dirs and
     * sets owner-only permissions on the file so it isn't world-readable. If anything
     * fails we log the password ourselves rather than dropping it on the floor.
     */
    private void recordPassword(String username, String rawPassword) {
        String line = String.format(
                "%s  username=%s  password=%s%n",
                OffsetDateTime.now(), username, rawPassword);
        try {
            if (passwordsFile.getParent() != null) {
                Files.createDirectories(passwordsFile.getParent());
            }
            Files.writeString(passwordsFile, line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            // Best-effort permission tightening — ignored on non-POSIX file systems.
            try {
                Files.setPosixFilePermissions(passwordsFile,
                        PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException | IOException ignored) {
                // Windows/dev box — fine.
            }
            log.warn("Bootstrap: password for '{}' written to {}. " +
                    "Read it once and delete the line after the user changes it.",
                    username, passwordsFile.toAbsolutePath());
        } catch (IOException e) {
            // Fallback: don't lose the credential, but keep it OUT of the structured logger
            // (log aggregators / alert channels would otherwise persist the password).
            // System.err goes to systemd's journal in plaintext only on this specific host.
            log.warn("Bootstrap: could not write {} ({}). Password for '{}' printed to stderr.",
                    passwordsFile, e.getMessage(), username);
            System.err.printf("[BOOTSTRAP-PASSWORD] username=%s password=%s%n", username, rawPassword);
        }
    }

    private record UserSpec(String username, Role role, String displayName, String fixedPassword) {}
}
