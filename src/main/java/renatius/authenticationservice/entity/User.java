package renatius.authenticationservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "users", schema = "user_schema")
@Getter
@Setter
@RequiredArgsConstructor
public class User{

    @Id
    private UUID id;

    @Column(name = "username" ,nullable = false, unique = true)
    private String username;

    @Column(name = "email",unique = true, nullable = false)
    private String email;

    @Column (name = "password", nullable = false, unique = true)
    private String password;

}
