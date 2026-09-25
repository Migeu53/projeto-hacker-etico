package br.com.amigo.biblioteca.repository;
import br.com.amigo.biblioteca.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface LoanRepository extends JpaRepository<Loan,Long>{ List<Loan> findByUserIdOrderByIdDesc(Long userId); }