package com.fajurion.learn.controller.account;

import com.fajurion.learn.repository.account.Account;
import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.invite.InviteRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.util.CustomException;
import com.fajurion.learn.util.AccountUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    // Repository for getting account data
    private final AccountRepository accountRepository;

    // Service for checking/creating sessions
    private final SessionService sessionService;

    // Repository for checking invites
    private final InviteRepository inviteRepository;

    @Autowired
    public AccountController(AccountRepository accountRepository, SessionService sessionService, InviteRepository inviteRepository) {
        this.accountRepository = accountRepository;
        this.sessionService = sessionService;
        this.inviteRepository = inviteRepository;
    }

    /**
     * Login endpoint
     *
     * @param loginForm Login form from the client
     * @return Login response
     */
    @PostMapping("/login")
    @CrossOrigin
    public Mono<LoginResponse> login(@RequestBody LoginForm loginForm) {

        // Check if body is valid
        if(loginForm.username() == null || loginForm.password() == null) {
            return Mono.just(new LoginResponse(false, false, "login.invalid"));
        }

        // Check if user exists
        return accountRepository.getAccountsByUsername(loginForm.username()).hasElements().flatMap(success -> {

            // false: Invalid username
            if(!success) {
                return Mono.error(new CustomException("login.invalid"));
            }

            // Get account to verify password
            return accountRepository.getAccountsByUsername(loginForm.username()).elementAt(0);
        }).flatMap(account -> {

            // Check password hash
            if(!AccountUtil.getHash(loginForm.username(), loginForm.password()).equals(account.getPassword())) {
                return Mono.error(new CustomException("login.invalid"));
            }

            // Create a new session
            return sessionService.generateSession(account.getId());
        }).map(session -> new LoginResponse(true, false, session.getToken()))

                // Error handling
                .onErrorResume(CustomException.class, error -> Mono.just(new LoginResponse(false, false, error.getMessage())))
                .onErrorReturn(new LoginResponse(false, true, "server.error"));
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
    @PostMapping("/register")
    @CrossOrigin
    public Mono<LoginResponse> register(@RequestBody RegisterForm registerForm) {

        // Check if request is valid
        if(registerForm.username() == null || registerForm.password() == null || registerForm.email() == null
            || registerForm.invite() == null) {
            return Mono.just(new LoginResponse(false, false, "invalid.request"));
        }

        // Check if values of form are valid

        // Check if username is valid
        if(registerForm.username().length() < 3 || registerForm.username().length() > 32) {
            return Mono.just(new LoginResponse(false, false, "invalid.request"));
        }

        // Check if password is valid
        if(registerForm.password().length() < 10 || registerForm.password().length() > 500) {
            return Mono.just(new LoginResponse(false, false, "invalid.request"));
        }

        // Check if email is valid
        if(!AccountUtil.isValidEmail(registerForm.email())) {
            return Mono.just(new LoginResponse(false, false, "register.email.invalid"));
        }

        // Check if username already exists
        return accountRepository.getAccountByUsernameIgnoreCase(registerForm.username()).hasElement().flatMap(exists -> {

            if(exists) {
                return Mono.error(new CustomException("register.username.exists"));
            }

            // Check if email already exists
            return accountRepository.getAccountByEmailIgnoreCase(registerForm.email());
        }).hasElement().flatMap(email -> {

            if(email) {
                return Mono.error(new CustomException("register.email.exists"));
            }

            // Check if invite exists
            return inviteRepository.getInvitesByCode(registerForm.invite()).collectList();
        }).flatMap(invites -> {

            if(invites.isEmpty()) {
                return Mono.error(new CustomException("register.invite.not_found"));
            }

            // Delete invite
            return inviteRepository.delete(invites.get(0)).thenReturn(invites.get(0));
        }).flatMap(invite -> accountRepository.save(new Account(registerForm.username(), registerForm.email(), "User", AccountUtil.getHash(registerForm.username(), registerForm.password()), "", invite.getCreator()))).flatMap(account -> {

            // Create new session
            return sessionService.generateSession(account.getId());
        }).flatMap(session -> {

            // Return login response with token
            return Mono.just(new LoginResponse(true, false, session.getToken()));
        })
                .onErrorResume(CustomException.class, e -> Mono.just(new LoginResponse(false, false, e.getMessage())))
                .onErrorResume(e -> Mono.just(new LoginResponse(false, true, "server.error")))
            .onErrorReturn(new LoginResponse(false, true, "server.error"));
    }

    // Record for register form
    public record RegisterForm(String email, String username, String password, String invite) {}
}
