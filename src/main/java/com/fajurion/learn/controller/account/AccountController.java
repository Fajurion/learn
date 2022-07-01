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

    @RequestMapping("/login")
    @ResponseBody
    public Mono<LoginResponse> test(@RequestBody LoginForm login) {

        // Check if user exists
        return accountRepository.getAccountsByUsername(login.username()).hasElements().flatMap(success -> {

            // false: Invalid username
            if(!success) {
                return Mono.error(new RuntimeException("Invalid username or password."));
            }

            // Get account to verify password
            return accountRepository.getAccountsByUsername(login.username()).elementAt(0);
        }).flatMap(account -> {

            // Check password hash
            if(!PasswordUtil.getHash(login.username(), login.password()).equals(account.getPassword())) {
                return Mono.error(new RuntimeException("Invalid username or password."));
            }

            // Create a new session
            return sessionRepository.save(new Session(UUID.randomUUID().toString(), login.username(), ""));
        }).flatMap(session -> Mono.just(new LoginResponse(true, false, session.getToken())))

                // Error handling
                .onErrorResume(RuntimeException.class, error -> Mono.just(new LoginResponse(false, false, error.getMessage())))
                .onErrorResume(error -> Mono.just(new LoginResponse(false, true, error.getMessage())));
    }

    public record LoginForm(String username, String password) {}
    public record LoginResponse(boolean success, boolean error, String token) {}

    @RequestMapping("/register/start")
    public Mono<String> startRegister() {
        return Mono.just("test");
    }

}
