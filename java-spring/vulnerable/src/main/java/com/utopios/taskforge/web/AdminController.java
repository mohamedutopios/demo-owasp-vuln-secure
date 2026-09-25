package com.utopios.taskforge.web;

import com.utopios.taskforge.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminController {

    private final UserRepository users;

    public AdminController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/admin/users")
    public String users(Model model) {
        model.addAttribute("users", users.findAll());
        return "admin_users";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String delete(@PathVariable Long id) {
        users.deleteById(id);
        return "redirect:/admin/users";
    }
}
