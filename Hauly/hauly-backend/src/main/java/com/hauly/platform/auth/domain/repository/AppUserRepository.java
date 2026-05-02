package com.hauly.platform.auth.domain.repository;

import com.hauly.platform.auth.domain.model.AppUser;
import com.hauly.platform.auth.domain.model.Email;

import java.util.Optional;

/**
 * Domain repository interface for AppUser aggregate.
 * Domain layer: NO Spring Data, JPA, or framework imports allowed here.
 * Infrastructure provides the implementation via dependency inversion.
 */
public interface AppUserRepository {

    Optional<AppUser> findByEmail(Email email);

    Optional<AppUser> findById(Long id);

    AppUser save(AppUser user);

    long count();
}
