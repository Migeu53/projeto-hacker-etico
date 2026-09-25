package br.com.amigo.biblioteca.repository;
import br.com.amigo.biblioteca.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User,Long>{ Optional<User> findByEmailIgnoreCase(String email); boolean existsByEmailIgnoreCase(String email); }