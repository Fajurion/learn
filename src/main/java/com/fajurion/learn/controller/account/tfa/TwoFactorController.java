package com.fajurion.learn.controller.account.tfa;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.util.CustomException;
import com.fajurion.learn.util.TwoFactorUtil;
import com.google.zxing.WriterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
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

    @Autowired
    public TwoFactorController(SessionService sessionService,
                                     AccountRepository accountRepository) {
        this.sessionService = sessionService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/api/settings/tfa/activate")
    @CrossOrigin
    public Mono<TwoFactorActivateResponse> change(@RequestBody TwoFactorActivateForm form) {

        // Check if form is valid
        if(form.token() == null || form.currentPassword() == null) {
            return Mono.just(new TwoFactorActivateResponse(false, false, "invalid", ""));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.invalid"));
            }

            // TODO: Activate TwoFactor
        });
    }

    // Record for requesting tfa activation
    public record TwoFactorActivateForm(String token, String currentPassword) {}

    // Response to activating tfa
    public record TwoFactorActivateResponse(boolean success, boolean error, String message, String secret) {}

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
