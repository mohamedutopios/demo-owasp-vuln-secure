package com.utopios.taskforge.config;

import com.utopios.taskforge.model.Task;
import com.utopios.taskforge.model.User;
import com.utopios.taskforge.repository.TaskRepository;
import com.utopios.taskforge.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository users;
    private final TaskRepository tasks;

    public DataLoader(UserRepository users, TaskRepository tasks) {
        this.users = users;
        this.tasks = tasks;
    }

    private static String md5(String s) {
        return DigestUtils.md5DigestAsHex(s.getBytes(StandardCharsets.UTF_8));
    }

    private User user(String u, String p, String r, String iban) {
        User x = new User();
        x.setUsername(u); x.setPassword(md5(p)); x.setRole(r); x.setIban(iban);
        return x;
    }

    private Task task(String t, String d, String o) {
        Task x = new Task(); x.setTitle(t); x.setDescription(d); x.setOwner(o);
        return x;
    }

    @Override
    public void run(String... args) {
        users.save(user("admin", "admin", "ADMIN", "FR7630006000011234567890189"));
        users.save(user("alice", "password1", "USER", "FR1420041010050500013M02606"));
        users.save(user("bob", "hunter2", "USER", "FR7630004000031234567890143"));
        tasks.save(task("Preparer la release", "Publier la version 1.2 du portail", "alice"));
        tasks.save(task("Revue budget", "Valider les depenses Q3", "alice"));
        tasks.save(task("Note RH confidentielle", "Augmentation de bob a valider", "admin"));
        tasks.save(task("Migration serveur", "Basculer sur le nouveau datacenter", "bob"));
        System.out.println("Base initialisee : admin/admin, alice/password1, bob/hunter2");
    }
}
