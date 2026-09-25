package com.utopios.taskforge.web;

import com.utopios.taskforge.model.User;
import com.utopios.taskforge.repository.UserRepository;
import com.utopios.taskforge.security.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.regex.Pattern;

@Controller
public class AuthController {

    private static final Logger AUDIT = LoggerFactory.getLogger("taskforge.audit");
    // A06/A07 - politique : 12 caracteres minimum, majuscule, minuscule, chiffre.
    private static final Pattern STRONG_PWD =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{12,}$");

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final CryptoService crypto;

    public AuthController(UserRepository users, PasswordEncoder encoder, CryptoService crypto) {
        this.users = users;
        this.encoder = encoder;
        this.crypto = crypto;
    }

    // La soumission du formulaire de connexion est traitee par Spring Security.
    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam(required = false) String iban,
                           Model model) {
        String u = username == null ? "" : username.trim();
        if (u.isEmpty() || password == null || !STRONG_PWD.matcher(password).matches()) {
            model.addAttribute("error", "Mot de passe trop faible : 12 caracteres minimum, "
                    + "avec majuscule, minuscule et chiffre.");
            return "register";
        }
        if (users.existsByUsername(u)) {
            model.addAttribute("error", "Identifiant indisponible.");
            return "register";
        }
        User user = new User();
        user.setUsername(u);
        user.setPassword(encoder.encode(password));          // A04 - BCrypt
        user.setRole("USER");                                // A06 - jamais depuis le formulaire
        user.setIban((iban != null && !iban.isBlank()) ? crypto.encrypt(iban.trim()) : null);
        users.save(user);
        AUDIT.info("compte_cree user={}", u);                // A09 - pas de mot de passe
        return "redirect:/login";
    }

    @GetMapping("/reset")
    public String resetForm() {
        return "reset";
    }

    @PostMapping("/reset")
    public String reset(@RequestParam String email, Model model) {
        // A06 - reponse identique que le compte existe ou non (anti-enumeration).
        // Le jeton reel serait aleatoire, a duree limitee et transmis hors bande.
        AUDIT.info("reset_demande");
        model.addAttribute("message", "Si un compte correspond a cette adresse, un lien de "
                + "reinitialisation vient d'etre envoye.");
        return "reset";
    }
}
