package com.utopios.taskforge.security;

import com.utopios.taskforge.model.User;
import com.utopios.taskforge.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;
    private final LoginAttemptService attempts;

    public AppUserDetailsService(UserRepository users, LoginAttemptService attempts) {
        this.users = users;
        this.attempts = attempts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User u = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur inconnu"));

        // A07 - le compte est marque verrouille au-dela du quota de tentatives ;
        // Spring Security refuse alors l'authentification (LockedException).
        boolean nonLocked = !attempts.isBlocked(username);

        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(u.getPassword())                 // A04 - hash BCrypt en base
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole())))
                .accountLocked(!nonLocked)
                .build();
    }
}
