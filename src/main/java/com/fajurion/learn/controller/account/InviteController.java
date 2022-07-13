package com.fajurion.learn.controller.account;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.invite.Invite;
import com.fajurion.learn.repository.account.invite.InviteRepository;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.util.ConstantConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/invite")
public class InviteController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for getting account data
    private final AccountRepository accountRepository;

    // Repository for creating/checking invites
    private final InviteRepository inviteRepository;

    // Repository for getting rank permission level
    private final RankRepository rankRepository;

    @Autowired
    public InviteController(SessionService sessionService, AccountRepository accountRepository, InviteRepository inviteRepository, RankRepository rankRepository) {
        this.sessionService = sessionService;
        this.accountRepository = accountRepository;
        this.inviteRepository = inviteRepository;
        this.rankRepository = rankRepository;
    }

    @RequestMapping("/create")
    @ResponseBody @CrossOrigin
    public Mono<InviteCreateResponse> createInvite(@RequestBody InviteCreateForm inviteCreateForm) {

        // Atomic reference for user account id
        AtomicReference<Integer> userID = new AtomicReference<>();

        // Check if session is valid
        return sessionService.checkAndRefreshSession(inviteCreateForm.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // Get account from session
            return accountRepository.findById(session.getAccount());
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
            if(rank.getLevel() < ConstantConfiguration.PERMISSION_LEVEL_CREATE_INVITE) {
                return Mono.error(new RuntimeException("no_permission"));
            }

            // Create invite
            Invite invite = new Invite(UUID.randomUUID().toString(), userID.get(), "");

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
