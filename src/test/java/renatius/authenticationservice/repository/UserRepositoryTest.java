package renatius.authenticationservice.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import renatius.authenticationservice.entity.User;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save user and find by email")
    void testFindByEmail() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("john@example.com");
        user.setUsername("john");
        user.setPassword("secret");
        userRepository.save(user);
        Optional<User> found = userRepository.findByEmail("john@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("john");
    }

    @Test
    @DisplayName("Should save user and find by ID")
    void testFindById() {
        UUID id = UUID.randomUUID();

        User user = new User();
        user.setId(id);
        user.setEmail("anna@example.com");
        user.setUsername("anna");
        user.setPassword("pw");

        userRepository.save(user);

        Optional<User> found = userRepository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("anna@example.com");
    }

    @Test
    @DisplayName("Should check existence by email")
    void testExistsByEmail() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("mark@example.com");
        user.setUsername("mark");
        user.setPassword("qwerty");
        userRepository.save(user);

        boolean exists = userRepository.existsByEmail("mark@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should check existence by username")
    void testExistsByUsername() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("kate@example.com");
        user.setUsername("kate");
        user.setPassword("pw");
        userRepository.save(user);

        boolean exists = userRepository.existsByUsername("kate");

        assertThat(exists).isTrue();
    }
}

