package com.fajurion.learn.repository.account.session;

import com.fajurion.learn.util.ConstantConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SessionService {

    // Repository for accessing sessions
    private final SessionRepository sessionRepository;

    @Autowired
    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public Mono<Session> generateSession(int userID) {

        // Generate a unique session identifier
        AtomicReference<String> token = new AtomicReference<>(generateRandomString());

        // Check if user has too many sessions
        return sessionRepository.countSessionsById(userID).flatMap(count -> {

            if(count >= ConstantConfiguration.MAXIMUM_CONCURRENT_SESSIONS) {
                return Mono.error(new RuntimeException("too_many_sessions"));
            }

            // Check if session token is already used
            return sessionRepository.findById(token.get());
        }).flatMap(session -> {

            if(session != null) {
                return Mono.error(new RuntimeException("try.again"));
            }

            return sessionRepository.save(new Session(token.get(), userID, System.currentTimeMillis()));
        });
    }

    public Mono<Session> checkAndRefreshSession(String token) {

        // Check if session exists
        return sessionRepository.findById(token).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // Check if session is timed out
            if(session.getCreation() + ConstantConfiguration.SESSION_TIMEOUT_DELAY < System.currentTimeMillis()) {

                // Delete session
                return sessionRepository.delete(session);
            }

            // Refresh session
            session.setCreation(System.currentTimeMillis());

            // Save session
            return sessionRepository.save(session);
        }).flatMap(obj -> {

            // Check if session has been saved
            if(obj instanceof Session) {
                return Mono.just((Session) obj);
            }

            return Mono.just(null);
        });
    }

    private String generateRandomString() {
        byte[] array = new byte[99];
        new Random().nextBytes(array);
        String generatedString = new String(array, StandardCharsets.UTF_8);
        System.out.println(generatedString);
        return generatedString;
    }

}
