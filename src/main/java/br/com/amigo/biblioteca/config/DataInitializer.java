package br.com.amigo.biblioteca.config;
import br.com.amigo.biblioteca.model.*; import br.com.amigo.biblioteca.repository.*;
import org.springframework.boot.CommandLineRunner; import org.springframework.context.annotation.*; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
@Configuration
public class DataInitializer {
 @Bean CommandLineRunner seed(UserRepository users,LoanRepository loans,BCryptPasswordEncoder enc){return args->{if(users.count()==0){
  User admin=users.save(new User("Bibliotecária Ana","admin@biblioteca.local",enc.encode("Admin123!"),"ADMIN","Administração"));
  User leo=users.save(new User("Leonardo Souza","leo@biblioteca.local",enc.encode("Leo12345!"),"USER","Informática"));
  User bia=users.save(new User("Beatriz Lima","bia@biblioteca.local",enc.encode("Bia12345!"),"USER","Design"));
  loans.save(new Loan(leo.getId(),leo.getName(),"1984","EMPRESTADO","Renovação permitida uma vez."));
  loans.save(new Loan(bia.getId(),bia.getName(),"Dom Casmurro","EMPRESTADO","Pendência interna de devolução."));
  loans.save(new Loan(admin.getId(),admin.getName(),"Manual Interno da Biblioteca","RESTRITO","FLAG{ARQUIVO_RESTRITO_EXPOSTO}"));
 }};}
}