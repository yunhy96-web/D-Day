package com.hauly.platform.auth.infrastructure.persistence;

import com.hauly.platform.auth.domain.model.AppUser;
import com.hauly.platform.auth.domain.model.Email;
import com.hauly.platform.auth.domain.repository.AppUserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA implementation of the domain AppUserRepository interface.
 * Extends both JpaRepository (Spring Data) and the domain interface (dependency inversion).
 * Infrastructure layer: Spring Data imports allowed here.
 *
 * JpaRepository<AppUser, Long> already satisfies:
 *   - findById(Long)  → Optional<AppUser>   (from CrudRepository)
 *   - save(AppUser)   → AppUser             (from CrudRepository)
 *   - count()         → long                (from CrudRepository)
 *
 * We only need to bridge Email VO → String for the findByEmail domain method.
 */
@Repository
public interface JpaAppUserRepository extends JpaRepository<AppUser, Long>, AppUserRepository {

    /**
     * Derived query by email string — Spring Data generates the SQL.
     */
    Optional<AppUser> findByEmail(String email);

    /**
     * Bridges the domain interface method (Email VO) to the string-based derived query.
     */
    default Optional<AppUser> findByEmail(Email email) {
        return findByEmail(email.value());
    }
}
