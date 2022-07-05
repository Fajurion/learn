package com.fajurion.learn.frontend.controller.account;

import com.fajurion.learn.repository.account.Account;
import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.invite.InviteRepository;
import com.fajurion.learn.repository.account.session.Session;
import com.fajurion.learn.repository.account.session.SessionRepository;
import com.fajurion.learn.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private InviteRepository inviteRepository;

    /**
     * Login endpoint
     *
     * @param loginForm Login form from the client
     * @return Login response
     */
    @RequestMapping("/login")
    @ResponseBody
    public Mono<LoginResponse> login(@RequestBody LoginForm loginForm) {

        // Check if user exists
        return accountRepository.getAccountsByUsername(loginForm.username()).hasElements().flatMap(success -> {

            // false: Invalid username
            if(!success) {
                return Mono.error(new RuntimeException("login.invalid"));
            }

            // Get account to verify password
            return accountRepository.getAccountsByUsername(loginForm.username()).elementAt(0);
        }).flatMap(account -> {

            // Check password hash
            if(!PasswordUtil.getHash(loginForm.username(), loginForm.password()).equals(account.getPassword())) {
                return Mono.error(new RuntimeException("login.invalid"));
            }

            // Create a new session
            return sessionRepository.save(new Session(UUID.randomUUID().toString(), account.getId(), ""));
        }).flatMap(session -> Mono.just(new LoginResponse(true, false, session.getToken())))

                // Error handling
                .onErrorResume(RuntimeException.class, error -> Mono.just(new LoginResponse(false, false, error.getMessage())))
                .onErrorResume(error -> Mono.just(new LoginResponse(false, true, error.getMessage())));
    }

    // Record for login form
    public record LoginForm(String username, String password) {}

    // Record for login/register response
    public record LoginResponse(boolean success, boolean error, String data) {}

    /**
     * Endpoint for account registration
     *
     * @param registerForm Form for account registration
     * @return Register response
     */
    @RequestMapping("/register")
    @ResponseBody
    public Mono<LoginResponse> register(@RequestBody RegisterForm registerForm) {

        // Check if username already exists
        return accountRepository.getAccountByUsernameIgnoreCase(registerForm.username()).hasElement().flatMap(exists -> {

            if(exists) {
                return Mono.error(new RuntimeException("register.username.exists"));
            }

            // Check if email already exists
            return accountRepository.getAccountByEmailIgnoreCase(registerForm.email());
        }).hasElement().flatMap(exists -> {

            if(exists) {
                return Mono.error(new RuntimeException("register.email.exists"));
            }

            // Check if invite exists
            return inviteRepository.findById(registerForm.invite());
        }).flatMap(invite -> {

            if(invite == null) {
                return Mono.error(new RuntimeException("register.invite.not_found"));
            }

            // Delete invite
            return inviteRepository.delete(invite);
        }).flatMap(invite -> {

            // Register account
            return accountRepository.save(new Account(registerForm.username(), registerForm.email(), "User", PasswordUtil.getHash(registerForm.username(), registerForm.password()), ""));
        }).flatMap(account -> {

            // Create new session
            return sessionRepository.save(new Session(UUID.randomUUID().toString(), account.getId(), ""));
        }).map(session -> {

            // Return login response with token
            return new LoginResponse(true, false, session.getToken());
        }).onErrorResume(RuntimeException.class, e -> Mono.just(new LoginResponse(false, false, e.getMessage())))
                .onErrorResume(e -> Mono.just(new LoginResponse(false, true, "server.error")));
    }

    // Record for register form
    public record RegisterForm(String email, String username, String password, String invite) {}
}
