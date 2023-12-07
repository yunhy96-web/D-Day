package DdaySpringBoot.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

@Table(name="users")
@NoArgsConstructor
@Getter
@Entity
public class User{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name ="id", updatable = false)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Builder
    public User(String email, String password, String auth){
        this.email = email;
        this.password = password;
    }
    //여기 아래 부분들이 다 유저디테일들

}
