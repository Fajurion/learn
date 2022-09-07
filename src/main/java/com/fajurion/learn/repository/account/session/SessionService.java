package com.fajurion.learn.repository.account.session;

import com.fajurion.learn.repository.account.Account;
import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.util.Configuration;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SessionService {

    // Repository for accessing sessions
    private final SessionRepository sessionRepository;

    // Repository for getting account data
    private final AccountRepository accountRepository;

    // Repository for getting ranks
    private final RankRepository rankRepository;

    @Autowired
    public SessionService(SessionRepository sessionRepository,
                          AccountRepository accountRepository,
                          RankRepository rankRepository) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.rankRepository = rankRepository;
    }

    public Mono<Session> generateSession(int userID, String type) {

        // Generate a unique session identifier
        AtomicReference<String> token = new AtomicReference<>(generateRandomString(99));

        // Get all sessions of the user
        return sessionRepository.getSessionsByAccount(userID).collectList().flatMap(list -> {

            // Sessions to delete from the database
            ArrayList<Session> toDelete = new ArrayList<>();

            // Remove old sessions
            for(Session session : list) {
                if(session.getCreation() + TimeUnit.DAYS.toMillis(1) < System.currentTimeMillis()) {
                    toDelete.add(session);
                }
            }

            // Check if user has too many sessions
            if (list.size() - toDelete.size() >= Configuration.settings.get("max.sessions")) {
                return Mono.error(new CustomException("too_many_sessions"));
            }

            // Check if session token is already used
            return Mono.zip(
                    // Delete all tfa sessions
                    sessionRepository.deleteAllByAccountAndType(userID, "tfa").thenReturn("test"),

                    sessionRepository.save(new Session(token.get(), userID, type.equals("tfa") ? System.currentTimeMillis() - 10000 : System.currentTimeMillis(), type)),
                    sessionRepository.deleteAll(toDelete).thenReturn(new Session("d", -1, -1, "")));
        }).map(Tuple3::getT2);
    }

    public Mono<Session> checkAndRefreshSession(String token) {

        // Check if session exists
        return sessionRepository.getSessionsByTokenAndType(token, "access").collectList().flatMap(sessions -> {

            // Check if there are two sessions with the same token (really unlikely)
            if(sessions.size() > 1) {
                return Mono.error(new CustomException("session.expired"));
            }

            if(sessions.isEmpty()) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Check if session is timed out
            if(sessions.get(0).getCreation() + Configuration.settings.get("session.timeout") < System.currentTimeMillis()) {

                // Delete session
                return sessionRepository.delete(sessions.get(0)).thenReturn("test");
            }

            // Refresh session
            sessions.get(0).setCreation(System.currentTimeMillis());

            // Save session
            return sessionRepository.save(sessions.get(0));
        }).flatMap(obj -> {

            // Check if session has been saved
            if(obj instanceof Session) {
                return Mono.just((Session) obj);
            }

            return Mono.just(null);
        });
    }

    /**
     * Combines session flow and permission
     * flow with the rank system into one
     *
     * @param token Session token
     * @param permission Permission
     * @return If the user has the permission
     */
    public Mono<Boolean> checkPermissionAndRefresh(String token, String permission) {

        // Check session token
        return checkAndRefreshSession(token).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get account
            return accountRepository.findById(session.getAccount()).onErrorReturn(new Account("", "", "", "", "", -1));
        }).flatMap(account -> {

            // Check if account exists
            if(account.getInvitor() == -1) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get rank
            return rankRepository.getRankByName(account.getRank());
        }).map(rank -> {

            // Check if rank has permission
            return Configuration.permissions.get(permission) > rank.getLevel();
        });
    }

    private String generateRandomString(int length) {
        // Alphabet
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        // StringBuilder for storing all random characters
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {

            // Get random number from string and add it to stringbuilder
            sb.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length())));
        }

        return sb.toString();
    }

}
