package renatius.authenticationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import renatius.authenticationservice.entity.User;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for accessing {@link User} entities.
 *
 * <p>Provides methods to find users by email, username, or ID,
 * and to check if a user already exists by email or username.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
