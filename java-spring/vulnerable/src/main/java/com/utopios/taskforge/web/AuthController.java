package com.utopios.taskforge.web;

import com.utopios.taskforge.model.User;
import com.utopios.taskforge.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Controller
public class AuthController {

    private final UserRepository users;

    public AuthController(UserRepository users) {
        this.users = users;
    }

    private static String md5(String s) {
        return DigestUtils.md5DigestAsHex(s.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/login")
    public String loginForm() { return "login"; }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        @RequestParam(required = false) String remember,
                        HttpSession session, HttpServletResponse resp, Model model) {
        System.out.println("Tentative de connexion user=" + username + " pass=" + password);
        User u = users.findByUsername(username);
        if (u != null && u.getPassword().equals(md5(password))) {
            session.setAttribute("user", u.getUsername());
            session.setAttribute("role", u.getRole());
            resp.addCookie(new Cookie("token", md5(username)));
            if (remember != null) {
                String v = Base64.getEncoder().encodeToString(
                        (username + ":" + md5(password)).getBytes(StandardCharsets.UTF_8));
                Cookie c = new Cookie("remember", v);
                c.setMaxAge(2592000);
                resp.addCookie(c);
            }
            return "redirect:/tasks";
        }
        model.addAttribute("error", "Identifiants invalides");
        return "login";
    }

    @GetMapping("/register")
    public String registerForm() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password,
                           @RequestParam(defaultValue = "USER") String role,
                           @RequestParam(required = false) String iban) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(md5(password));
        u.setRole(role);
        u.setIban(iban);
        users.save(u);
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("user");
        return "redirect:/login";
    }

    @GetMapping("/reset")
    public String resetForm() { return "reset"; }

    @PostMapping("/reset")
    public String reset(@RequestParam String email, Model model) {
        User u = users.findByUsername(email);
        if (u != null) {
            model.addAttribute("message", "Lien envoye. Jeton : " + md5(email));
        } else {
            model.addAttribute("message", "Aucun compte associe a cette adresse.");
        }
        return "reset";
    }
}
