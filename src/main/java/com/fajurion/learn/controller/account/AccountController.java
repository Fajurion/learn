package com.fajurion.learn.controller.account;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.session.Session;
import com.fajurion.learn.repository.account.session.SessionRepository;
import com.fajurion.learn.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SessionRepository sessionRepository;

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
                return Mono.error(new RuntimeException("Invalid username or password."));
            }

            // Get account to verify password
            return accountRepository.getAccountsByUsername(loginForm.username()).elementAt(0);
        }).flatMap(account -> {

            // Check password hash
            if(!PasswordUtil.getHash(loginForm.username(), loginForm.password()).equals(account.getPassword())) {
                return Mono.error(new RuntimeException("Invalid username or password."));
            }

            // Create a new session
            return sessionRepository.save(new Session(UUID.randomUUID().toString(), loginForm.username(), ""));
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
    public Mono<LoginResponse> startRegister(@RequestBody RegisterForm registerForm) {
        return accountRepository.getAccountsByUsername(registerForm.username()).hasElements().flatMap(exists -> {

            if(exists) {
                return Mono.error(new RuntimeException("Username already exists."));
            }

            return Mono.just(new LoginResponse(false, true, "d"));
        }).onErrorResume(e -> Mono.just(new LoginResponse(false, false, e.getMessage())));
    }

    // Record for register form
    public record RegisterForm(String email, String username, String password, String invite) {}
}
