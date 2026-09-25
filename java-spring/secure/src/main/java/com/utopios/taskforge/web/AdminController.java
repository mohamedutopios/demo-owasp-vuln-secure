package com.utopios.taskforge.web;

import com.utopios.taskforge.model.User;
import com.utopios.taskforge.repository.UserRepository;
import com.utopios.taskforge.security.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
// A01 - controle d'acces fonctionnel : reserve au role ADMIN (defense en profondeur
// en plus de la regle d'URL definie dans SecurityConfig).
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger AUDIT = LoggerFactory.getLogger("taskforge.audit");

    private final UserRepository users;
    private final CryptoService crypto;

    public AdminController(UserRepository users, CryptoService crypto) {
        this.users = users;
        this.crypto = crypto;
    }

    @GetMapping("/admin/users")
    public String users(Model model, Authentication auth) {
        List<Map<String, Object>> view = new ArrayList<>();
        for (User u : users.findAll()) {
            // A04 - IBAN dechiffre puis masque (4 derniers caracteres seulement).
            String iban = crypto.decrypt(u.getIban() == null ? "" : u.getIban());
            String masked = iban.isEmpty() ? "-" : "**** " + iban.substring(Math.max(0, iban.length() - 4));
            view.add(Map.of("id", u.getId(), "username", u.getUsername(),
                    "role", u.getRole(), "iban", masked));
        }
        model.addAttribute("users", view);
        model.addAttribute("user", auth.getName());
        return "admin_users";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth) {
        User me = users.findByUsername(auth.getName()).orElse(null);
        if (me != null && me.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        users.deleteById(id);
        AUDIT.info("user_supprime par={} cible={}", auth.getName(), id);
        return "redirect:/admin/users";
    }
}
