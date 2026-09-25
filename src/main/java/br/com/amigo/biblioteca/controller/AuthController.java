package br.com.amigo.biblioteca.controller;
import br.com.amigo.biblioteca.model.User; import br.com.amigo.biblioteca.repository.UserRepository; import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.stereotype.Controller; import org.springframework.ui.Model; import org.springframework.web.bind.annotation.*;
@Controller
public class AuthController {
 private final UserRepository users; private final BCryptPasswordEncoder enc;
 public AuthController(UserRepository users,BCryptPasswordEncoder enc){this.users=users;this.enc=enc;}
 @GetMapping("/") public String home(HttpSession s,Model m){m.addAttribute("logged",s.getAttribute("userId")!=null);m.addAttribute("name",s.getAttribute("userName"));return "index";}
 @GetMapping("/login") public String login(){return "login";}
 @PostMapping("/login") public String doLogin(@RequestParam String email,@RequestParam String password,HttpSession s,Model m){var u=users.findByEmailIgnoreCase(email.trim()); if(u.isEmpty()||!enc.matches(password,u.get().getPasswordHash())){m.addAttribute("error","E-mail ou senha inválidos.");return "login";} User x=u.get();s.setAttribute("userId",x.getId());s.setAttribute("userName",x.getName());s.setAttribute("role",x.getRole());return "redirect:/dashboard";}
 @GetMapping("/register") public String reg(){return "register";}
 @PostMapping("/register") public String doReg(@RequestParam String name,@RequestParam String email,@RequestParam String password,@RequestParam(defaultValue="Não informado") String course,Model m){email=email.trim().toLowerCase(); if(name.trim().length()<2||!email.contains("@")||users.existsByEmailIgnoreCase(email)||password.length()<8){m.addAttribute("error","Verifique os dados. A senha precisa ter 8+ caracteres e o e-mail não pode estar em uso.");return "register";} users.save(new User(name.trim(),email,enc.encode(password),"USER",course.trim()));m.addAttribute("success","Cadastro realizado. Faça login.");return "login";}
 @GetMapping("/logout") public String logout(HttpSession s){s.invalidate();return "redirect:/";}
}