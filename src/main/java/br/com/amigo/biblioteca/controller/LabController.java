package br.com.amigo.biblioteca.controller;
import br.com.amigo.biblioteca.repository.*; import jakarta.servlet.http.HttpSession; import org.springframework.stereotype.Controller; import org.springframework.ui.Model; import org.springframework.web.bind.annotation.*;
@Controller @RequestMapping("/lab")
public class LabController {
 private final LoanRepository loans; private final UserRepository users;
 public LabController(LoanRepository loans,UserRepository users){this.loans=loans;this.users=users;}
 private boolean logged(HttpSession s){return s.getAttribute("userId")!=null;}
 @GetMapping public String home(HttpSession s){return logged(s)?"lab":"redirect:/login";}
 // Vulnerabilidade intencional 1: IDOR - não valida se o empréstimo pertence ao usuário autenticado.
 @GetMapping("/loan") public String loan(@RequestParam Long id,HttpSession s,Model m){if(!logged(s))return "redirect:/login";m.addAttribute("loan",loans.findById(id).orElse(null));return "lab-loan";}
 // Vulnerabilidade intencional 2: XSS refletido - o template usa th:utext propositalmente.
 @GetMapping("/search") public String search(@RequestParam(defaultValue="") String q,HttpSession s,Model m){if(!logged(s))return "redirect:/login";m.addAttribute("q",q);return "lab-search";}
 // Vulnerabilidade intencional 3: Broken Access Control - relatório deveria ser apenas do ADMIN.
 @GetMapping("/internal-report") public String report(HttpSession s,Model m){if(!logged(s))return "redirect:/login";m.addAttribute("users",users.findAll());m.addAttribute("loans",loans.findAll());m.addAttribute("flag","FLAG{RELATORIO_INTERNO_SEM_AUTORIZACAO}");return "lab-report";}
}