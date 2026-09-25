package com.utopios.taskforge.config;

import com.utopios.taskforge.model.Task;
import com.utopios.taskforge.model.User;
import com.utopios.taskforge.repository.TaskRepository;
import com.utopios.taskforge.repository.UserRepository;
import com.utopios.taskforge.security.CryptoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository users;
    private final TaskRepository tasks;
    private final PasswordEncoder encoder;
    private final CryptoService crypto;

    public DataLoader(UserRepository users, TaskRepository tasks,
                      PasswordEncoder encoder, CryptoService crypto) {
        this.users = users;
        this.tasks = tasks;
        this.encoder = encoder;
        this.crypto = crypto;
    }

    private User user(String u, String p, String r, String iban) {
        User x = new User();
        x.setUsername(u);
        x.setPassword(encoder.encode(p));   // A04 - BCrypt
        x.setRole(r);
        x.setIban(crypto.encrypt(iban));    // A04 - IBAN chiffre au repos
        return x;
    }

    private Task task(String t, String d, String o) {
        Task x = new Task();
        x.setTitle(t);
        x.setDescription(d);
        x.setOwner(o);
        return x;
    }

    @Override
    public void run(String... args) {
        // Mots de passe conformes a la politique (>=12, maj/min/chiffre).
        users.save(user("admin", "Admin!Passw0rd", "ADMIN", "FR7630006000011234567890189"));
        users.save(user("alice", "Alice!Passw0rd", "USER", "FR1420041010050500013M02606"));
        users.save(user("bob", "Bob!Passw0rd42", "USER", "FR7630004000031234567890143"));
        tasks.save(task("Preparer la release", "Publier la version 1.2 du portail", "alice"));
        tasks.save(task("Revue budget", "Valider les depenses Q3", "alice"));
        tasks.save(task("Note RH confidentielle", "Augmentation de bob a valider", "admin"));
        tasks.save(task("Migration serveur", "Basculer sur le nouveau datacenter", "bob"));
    }
}
