package com.hauly.platform.auth.domain.repository;

import com.hauly.platform.auth.domain.model.AppUser;
import com.hauly.platform.auth.domain.model.Username;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for AppUser aggregate.
 * Domain layer: NO Spring Data, JPA, or framework imports allowed here.
 * Infrastructure provides the implementation via dependency inversion.
 */
public interface AppUserRepository {

    Optional<AppUser> findByUsername(Username username);

    Optional<AppUser> findById(Long id);

    /** Batch lookup — returns rows in arbitrary order; missing ids are simply absent. */
    List<AppUser> findAllById(Iterable<Long> ids);

    AppUser save(AppUser user);

    long count();
}
