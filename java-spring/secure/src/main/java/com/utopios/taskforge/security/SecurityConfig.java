package com.utopios.taskforge.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger AUDIT = LoggerFactory.getLogger("taskforge.audit");

    private final LoginAttemptService attempts;

    public SecurityConfig(LoginAttemptService attempts) {
        this.attempts = attempts;
    }

    // A04 - BCrypt (cout 12) pour le stockage et la verification des mots de passe.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // A01 - autorisation centralisee, refus par defaut.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/reset", "/error").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // A07 - authentification par formulaire geree par le framework.
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler())
                .failureHandler(failureHandler())
                .permitAll()
            )
            // A07 - deconnexion en POST (protegee CSRF), invalidation de session.
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            // A07 - session recreee a la connexion (anti-fixation).
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(sf -> sf.newSession())
            )
            // A02 - en-tetes de securite.
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; style-src 'self' 'unsafe-inline'"))
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true).maxAgeInSeconds(31_536_000))
                .referrerPolicy(rp -> rp
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
            );
        // CSRF est actif par defaut (A08) ; H2 console desactivee cote properties (A02).
        return http.build();
    }

    // A09 - journalisation des succes sans donnee sensible ; reinitialise le compteur.
    private AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            attempts.reset(username);
            AUDIT.info("login_ok user={}", username);
            response.sendRedirect(request.getContextPath() + "/tasks");
        };
    }

    // A07/A09 - enregistre l'echec (anti-force brute) et journalise sans mot de passe.
    private AuthenticationFailureHandler failureHandler() {
        return (request, response, exception) -> {
            String username = request.getParameter("username");
            if (exception instanceof LockedException) {
                AUDIT.warn("login_bloque_ratelimit user={}", username);
                response.sendRedirect(request.getContextPath() + "/login?error=locked");
                return;
            }
            attempts.recordFailure(username);
            AUDIT.warn("login_echec user={}", username);
            response.sendRedirect(request.getContextPath() + "/login?error=true");
        };
    }
}
