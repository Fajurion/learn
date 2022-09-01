package com.fajurion.learn.controller.account.tfa;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.account.tfa.TwoFactor;
import com.fajurion.learn.repository.account.tfa.TwoFactorRepository;
import com.fajurion.learn.util.AccountUtil;
import com.fajurion.learn.util.CustomException;
import com.fajurion.learn.util.TwoFactorUtil;
import com.google.zxing.WriterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
public class TwoFactorController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for getting account data
    private final AccountRepository accountRepository;

    // Repository for enabling two-factor authentication
    private final TwoFactorRepository twoFactorRepository;

    @Autowired
    public TwoFactorController(SessionService sessionService,
                               AccountRepository accountRepository,
                               TwoFactorRepository twoFactorRepository) {
        this.sessionService = sessionService;
        this.accountRepository = accountRepository;
        this.twoFactorRepository = twoFactorRepository;
    }

    @PostMapping("/api/settings/tfa/activate")
    @CrossOrigin
    public Mono<TwoFactorActivateResponse> activate(@RequestBody TwoFactorForm form) {

        // Check if form is valid
        if(form.token() == null || form.currentPassword() == null) {
            return Mono.just(new TwoFactorActivateResponse(false, false, "invalid", "", ""));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.invalid"));
            }

            // Check password
            return accountRepository.findById(session.getAccount());
        }).flatMap(account -> {

            if(account.getPassword().equals(AccountUtil.getHash(account.getUsername(), account.getPassword()))) {
                return Mono.error(new CustomException("invalid.password"));
            }

            // Enable two factor
            return twoFactorRepository.save(new TwoFactor(account.getId(), TwoFactorUtil.generateSecretKey(), TwoFactorUtil.generateSecretKey()));
        }).flatMap(twoFactor -> {

            if(twoFactor == null) {
                return Mono.error(new CustomException("server.error"));
            }

            // Return response
            return Mono.just(new TwoFactorActivateResponse(true, false, "success", twoFactor.getSecret(), twoFactor.getBackup()));
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new TwoFactorActivateResponse(false, false, e.getMessage(), "", "")))
                .onErrorReturn(new TwoFactorActivateResponse(false, true, "server.error", "", ""));
    }

    // Response to activating tfa
    public record TwoFactorActivateResponse(boolean success, boolean error, String message, String secret, String backup) {}

    @PostMapping("/api/settings/tfa/deactivate")
    @CrossOrigin
    public Mono<TwoFactorDeactivateResponse> deactivate(@RequestBody TwoFactorForm form) {

        // Check if form is valid
        if(form.token() == null || form.currentPassword() == null) {
            return Mono.just(new TwoFactorDeactivateResponse(false, false, "invalid"));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

                    if(session == null) {
                        return Mono.error(new CustomException("session.invalid"));
                    }

                    // Check password
                    return accountRepository.findById(session.getAccount());
                }).flatMap(account -> {

                    if(account.getPassword().equals(AccountUtil.getHash(account.getUsername(), account.getPassword()))) {
                        return Mono.error(new CustomException("invalid.password"));
                    }

                    // Check if two factor is enabled
                    return twoFactorRepository.getTwoFactorByAccount(account.getId()).elementAt(0).onErrorReturn(new TwoFactor(-1, "", ""));
                }).flatMap(twoFactor -> {

                    if(twoFactor.getAccount() == -1) {
                        return Mono.error(new CustomException("not.enabled"));
                    }

                    // Turn off two factor and return response
                    return twoFactorRepository.delete(twoFactor).thenReturn(twoFactor);
                }).map(factor -> new TwoFactorDeactivateResponse(true, false, "success"))

                    // Error handling
                    .onErrorResume(CustomException.class, e -> Mono.just(new TwoFactorDeactivateResponse(false, false, e.getMessage())))
                    .onErrorReturn(new TwoFactorDeactivateResponse(false, true, "server.error"));
    }

    // Record for requesting tfa activation
    public record TwoFactorForm(String token, String currentPassword) {}

    // Response to deactivating tfa
    public record TwoFactorDeactivateResponse(boolean success, boolean error, String message) {}

    @GetMapping(value = "/api/settings/tfa/image", produces = "image/png")
    @CrossOrigin
    public ResponseEntity<InputStreamResource> getCodeImage(@RequestParam("data") String secret, @RequestParam("account") String account) throws WriterException, IOException {

        // Get the qr code
        BufferedImage image = TwoFactorUtil.createQRCode(TwoFactorUtil.getGoogleAuthenticatorURL(secret, account, "Learn Connect"));

        // Turn qr code into input stream
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        // Return new Input stream resource
        return ResponseEntity.ok()
                .body(new InputStreamResource(is));
    }

}
