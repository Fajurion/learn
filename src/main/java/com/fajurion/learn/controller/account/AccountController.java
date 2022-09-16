package com.fajurion.learn.controller.account;

import com.fajurion.learn.repository.account.Account;
import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.invite.InviteRepository;
import com.fajurion.learn.repository.account.session.SessionRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.account.tfa.TwoFactorRepository;
import com.fajurion.learn.util.AccountUtil;
import com.fajurion.learn.util.CustomException;
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

    // Repository for getting two factor data
    private final TwoFactorRepository twoFactorRepository;

    // Repository to delete sessions
    private final SessionRepository sessionRepository;

    @Autowired
    public AccountController(AccountRepository accountRepository,
                             SessionService sessionService,
                             InviteRepository inviteRepository,
                             TwoFactorRepository twoFactorRepository,
                             SessionRepository sessionRepository) {
        this.accountRepository = accountRepository;
        this.sessionService = sessionService;
        this.inviteRepository = inviteRepository;
        this.twoFactorRepository = twoFactorRepository;
        this.sessionRepository = sessionRepository;
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
        return accountRepository.getAccountsByUsername(loginForm.username()).elementAt(0).onErrorReturn(new Account("", "", "", "", "", -1)).flatMap(account -> {

            // invitor = -1: Invalid username
            if(account.getInvitor() == -1) {
                return Mono.error(new CustomException("login.invalid"));
            }

            // Get account to verify password and zip with tfa response
            return Mono.zip(accountRepository.getAccountsByUsername(loginForm.username()).elementAt(0),
                    twoFactorRepository.getTwoFactorByAccount(account.getId()).hasElements());
        }).flatMap(tuple -> {

            // Check password hash
            if(!AccountUtil.getHash(loginForm.username(), loginForm.password()).equals(tuple.getT1().getPassword())) {
                return Mono.error(new CustomException("login.invalid"));
            }

            // Create a new session and zip with tfa token
            return sessionService.generateSession(tuple.getT1().getId(), tuple.getT2() ? "tfa" : "access");
        }).map(session -> {

            // Return response
            return new LoginResponse(true, false, session.getType() + ":" + session.getToken());
        })

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
            return sessionService.generateSession(account.getId(), "access");
        }).flatMap(session -> {

            // Return login response with token
            return Mono.just(new LoginResponse(true, false, session.getToken()));
        })
                .onErrorResume(CustomException.class, e -> Mono.just(new LoginResponse(false, false, e.getMessage())))
                .onErrorReturn(new LoginResponse(false, true, "server.error"));
    }

    // Record for register form
    public record RegisterForm(String email, String username, String password, String invite) {}

    @PostMapping("/logout")
    @CrossOrigin
    public Mono<LogoutResponse> logOut(@RequestBody LogoutForm form) {

        // Check if form is valid
        if(form.token() == null) {
            return Mono.just(new LogoutResponse(false, false, "invalid"));
        }

        // Get session
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            // Check if session exists
            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Delete session
            return sessionRepository.delete(session).thenReturn("d");
        }).map(s -> new LogoutResponse(false, false, "session.expired")) // Automatic log out

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new LogoutResponse(false, false, e.getMessage())))
                .onErrorReturn(new LogoutResponse(false, true, "server.error"));
    }

    // Record for logging out
    public record LogoutForm(String token) {}

    // Response to logging out
    public record LogoutResponse(boolean success, boolean error, String message) {}

}
