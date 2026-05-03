package com.hauly.platform.auth.infrastructure.persistence;

import com.hauly.platform.auth.domain.model.AppUser;
import com.hauly.platform.auth.domain.model.Username;
import com.hauly.platform.auth.domain.repository.AppUserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA implementation of the domain AppUserRepository interface.
 *
 * JpaRepository<AppUser, Long> already satisfies:
 *   - findById(Long)            → Optional<AppUser>
 *   - findAllById(Iterable<Long>) → List<AppUser>
 *   - save(AppUser)             → AppUser
 *   - count()                   → long
 *
 * We only need to bridge Username VO → String for the findByUsername domain method.
 */
@Repository
public interface JpaAppUserRepository extends JpaRepository<AppUser, Long>, AppUserRepository {

    /**
     * Derived query by username string — Spring Data generates the SQL.
     */
    Optional<AppUser> findByUsername(String username);

    /**
     * Bridges the domain interface method (Username VO) to the string-based derived query.
     */
    default Optional<AppUser> findByUsername(Username username) {
        return findByUsername(username.value());
    }
}
