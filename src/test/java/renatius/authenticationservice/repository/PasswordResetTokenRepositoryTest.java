package renatius.authenticationservice.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import renatius.authenticationservice.entity.PasswordResetToken;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository repository;

    private PasswordResetToken createToken(String email, String token) {
        PasswordResetToken t = new PasswordResetToken();
        t.setEmail(email);
        t.setToken(token);
        t.setExpiresAt(LocalDateTime.now().plusHours(1));
        return repository.save(t);
    }

    @Test
    void findByTokenShouldReturnEntity() {
        createToken("john@example.com", "abc123");

        Optional<PasswordResetToken> res = repository.findByToken("abc123");

        assertThat(res).isPresent();
        assertThat(res.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void findByTokenShouldReturnEmptyWhenNotFound() {
        Optional<PasswordResetToken> res = repository.findByToken("notExists");

        assertThat(res).isEmpty();
    }

    @Test
    void deleteByEmailShouldRemoveAllTokensForEmail() {
        createToken("user@test.com", "t1");
        createToken("user@test.com", "t2");
        createToken("other@test.com", "t3");

        repository.deleteByEmail("user@test.com");

        assertThat(repository.findByToken("t1")).isEmpty();
        assertThat(repository.findByToken("t2")).isEmpty();
        assertThat(repository.findByToken("t3")).isPresent();
    }

    @Test
    void deleteByEmailShouldDoNothingIfEmailNotExists() {
        createToken("some@test.com", "t1");

        repository.deleteByEmail("unknown@test.com");

        assertThat(repository.findByToken("t1")).isPresent();
    }
}
