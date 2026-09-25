package br.com.amigo.biblioteca.controller;
import br.com.amigo.biblioteca.model.*; import br.com.amigo.biblioteca.repository.*; import jakarta.servlet.http.HttpSession; import org.springframework.stereotype.Controller; import org.springframework.ui.Model; import org.springframework.web.bind.annotation.*;
@Controller
public class AppController {
 private final UserRepository users; private final LoanRepository loans;
 public AppController(UserRepository users,LoanRepository loans){this.users=users;this.loans=loans;}
 private User current(HttpSession s){Object id=s.getAttribute("userId"); return id==null?null:users.findById((Long)id).orElse(null);}
 @GetMapping("/dashboard") public String dash(HttpSession s,Model m){User u=current(s);if(u==null)return "redirect:/login";m.addAttribute("user",u);m.addAttribute("loans",loans.findByUserIdOrderByIdDesc(u.getId()));m.addAttribute("isAdmin","ADMIN".equals(u.getRole()));return "dashboard";}
 @GetMapping("/profile") public String profile(HttpSession s,Model m){User u=current(s);if(u==null)return "redirect:/login";m.addAttribute("user",u);return "profile";}
 @GetMapping("/admin") public String admin(HttpSession s,Model m){User u=current(s);if(u==null)return "redirect:/login";if(!"ADMIN".equals(u.getRole())){m.addAttribute("message","Área exclusiva do administrador.");return "denied";}m.addAttribute("users",users.findAll());m.addAttribute("loans",loans.findAll());return "admin";}
}