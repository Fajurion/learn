package com.fajurion.learn.controller.account;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.util.AccountUtil;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class AccountSettingsController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for getting account data
    private final AccountRepository accountRepository;

    @Autowired
    public AccountSettingsController(SessionService sessionService,
                                     AccountRepository accountRepository) {
        this.sessionService = sessionService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/api/settings/password")
    @CrossOrigin
    public Mono<PasswordChangeResponse> changePassword(@RequestBody PasswordChangeForm form) {

        // Check if form is valid
        if(form.currentPassword() == null || form.token() == null || form.newPassword() == null || form.newPassword().length() < 10 || form.newPassword().length() > 500) {
            return Mono.just(new PasswordChangeResponse(false, false, "invalid"));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get account
            return accountRepository.findById(session.getAccount());
        }).flatMap(account -> {

            // Check if current password is correct
            if(!account.getPassword().equals(AccountUtil.getHash(account.getUsername(), form.currentPassword()))) {
                return Mono.error(new CustomException("invalid.password"));
            }

            // Set new password
            account.setPassword(AccountUtil.getHash(account.getUsername(), form.newPassword()));
            return accountRepository.save(account);
        }).flatMap(account -> {

            // Check if everything was successful
            if(account == null) {
                return Mono.error(new CustomException("server.error"));
            }

            // Return response
            return Mono.just(new PasswordChangeResponse(true, false, "success"));
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new PasswordChangeResponse(false, false, e.getMessage())))
                .onErrorReturn(new PasswordChangeResponse(false, true, "server.error"));
    }

    // Record for requesting a password change
    public record PasswordChangeForm(String token, String currentPassword, String newPassword) {}

    // Response to request a password change
    public record PasswordChangeResponse(boolean success, boolean error, String message) {}

}
