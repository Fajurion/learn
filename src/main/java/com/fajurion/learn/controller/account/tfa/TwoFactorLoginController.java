package com.fajurion.learn.controller.account.tfa;

import com.fajurion.learn.repository.account.session.Session;
import com.fajurion.learn.repository.account.session.SessionRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.account.tfa.TwoFactor;
import com.fajurion.learn.repository.account.tfa.TwoFactorRepository;
import com.fajurion.learn.util.CustomException;
import com.fajurion.learn.util.TwoFactorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class TwoFactorLoginController {

    // Repository for checking sessions
    private final SessionRepository sessionRepository;

    // Repository for enabling two-factor authentication
    private final TwoFactorRepository twoFactorRepository;

    // Service for creating sessions
    private final SessionService sessionService;

    @Autowired
    public TwoFactorLoginController(SessionRepository sessionRepository,
                                    TwoFactorRepository twoFactorRepository,
                                    SessionService sessionService) {
        this.sessionRepository = sessionRepository;
        this.twoFactorRepository = twoFactorRepository;
        this.sessionService = sessionService;
    }

    @PostMapping("/tfa")
    @CrossOrigin
    public Mono<TwoFactorLoginResponse> login(@RequestBody TwoFactorLoginForm form) {

        // Check if form is invalid
        if(form.code() == null || form.token() == null) {
            return Mono.just(new TwoFactorLoginResponse(false, false, "invalid"));
        }

        // Check if session is valid
        return sessionRepository.getSessionsByTokenAndType(form.token(), "tfa").elementAt(0).onErrorReturn(new Session("", -1, -1, ""))
                .flatMap(session -> {

            if(session.getAccount() == -1) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Check if delay between tries is over
            if(session.getCreation() + 10000 < System.currentTimeMillis()) {
                return Mono.error(new CustomException("wait"));
            }

            // Set new usage
            session.setCreation(System.currentTimeMillis());

            // Check the 2fa code and zip with session
            return Mono.zip(twoFactorRepository.getTwoFactorByAccount(session.getAccount()).elementAt(0).onErrorReturn(new TwoFactor(-1, "", "")),
                    sessionRepository.save(session));
        }).flatMap(tuple -> {

            // Check if two-factor authentication is enabled
            if(tuple.getT1().getAccount() == -1) {
                return Mono.error(new CustomException("server.error"));
            }

            // Check if the code matches
            if(!TwoFactorUtil.getTOTPCode(tuple.getT1().getSecret()).equals(form.code())) {
                return Mono.error(new CustomException("invalid.code"));
            }

            // Generate new session and zip with deletion of tfa token
            return Mono.zip(sessionService.generateSession(tuple.getT1().getAccount(), "access"),
                    sessionRepository.delete(tuple.getT2()));
        }).map(tuple -> {

            // Return response
            return new TwoFactorLoginResponse(true, false, tuple.getT1().getToken());
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new TwoFactorLoginResponse(false, false, e.getMessage())))
                .onErrorReturn(new TwoFactorLoginResponse(false, true, "server.error"));
    }

    // Record for logging in with 2fa
    public record TwoFactorLoginForm(String token, String code) {}

    // Response to requesting a 2fa login
    public record TwoFactorLoginResponse(boolean success, boolean error, String message) {}

}
