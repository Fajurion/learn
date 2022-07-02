package com.fajurion.learn.controller.account;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.invite.Invite;
import com.fajurion.learn.repository.account.invite.InviteRepository;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.repository.account.session.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@RequestMapping("/api/invite")
public class InviteController {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private InviteRepository inviteRepository;

    @Autowired
    private RankRepository rankRepository;

    @RequestMapping("/create")
    @ResponseBody
    public Mono<InviteCreateResponse> createInvite(@RequestBody InviteCreateForm inviteCreateForm) {

        // Atomic reference for user account id
        AtomicReference<Integer> userID = new AtomicReference<>();

        // Check if session is valid
        return sessionRepository.findById(inviteCreateForm.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // Get account from session
            return accountRepository.findById(session.getId());
        }).flatMap(account -> {

            if(account == null) {
                return Mono.error(new RuntimeException("session.expired.deleted"));
            }

            // Set id in atomic reference
            userID.set(account.getId());

            // Get the rank of the user
            return rankRepository.getRankByName(account.getRank());
        }).flatMap(rank -> {

            // Check if the rank has the required permission level
            if(rank.getLevel() < 90) {
                return Mono.error(new RuntimeException("no_permission"));
            }

            // Create invite
            Invite invite = new Invite(UUID.randomUUID().toString(), "", userID.get());

            // Save invite in repository
            return inviteRepository.save(invite);
        }).map(invite -> {

            // Return response with invite
            return new InviteCreateResponse(true, false, invite.getCode());
        }).onErrorResume(RuntimeException.class, e -> Mono.just(new InviteCreateResponse(false, false, e.getMessage())))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(new InviteCreateResponse(false, true, "server.error"));
                });
    }

    // Record for response when creating an invite
    public record InviteCreateResponse(boolean success, boolean error, String data) {}

    // Record for invite create form
    public record InviteCreateForm(String token) {}

}
