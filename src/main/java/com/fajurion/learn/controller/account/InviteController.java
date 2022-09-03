package com.fajurion.learn.controller.account;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.invite.Invite;
import com.fajurion.learn.repository.account.invite.InviteRepository;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.util.Configuration;
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
    @CrossOrigin
    public Mono<InviteCreateResponse> createInvite(@RequestBody InviteCreateForm inviteCreateForm) {

        // Check if session is valid
        return sessionService.checkAndRefreshSession(inviteCreateForm.token()).flatMap(session -> {

                    if (session == null) {
                        return Mono.error(new CustomException("session.expired"));
                    }

                    // Get account from session
                    return accountRepository.findById(session.getAccount());
                }).flatMap(account -> {

                    if (account == null) {
                        return Mono.error(new CustomException("session.expired"));
                    }

                    // Get the rank of the user and zip with account
                    return Mono.zip(rankRepository.getRankByName(account.getRank()), Mono.just(account));
                }).flatMap(tuple -> {

                    // Check if the rank has the required permission level
                    if (tuple.getT1().getLevel() < Configuration.permissions.get("create.invite")) {
                        return Mono.error(new CustomException("no_permission"));
                    }

                    // Create invite
                    Invite invite = new Invite(UUID.randomUUID().toString(), tuple.getT2().getId(), System.currentTimeMillis());

                    // Save invite in repository
                    return inviteRepository.save(invite);
                }).map(invite -> {

                    // Return response with invite
                    return new InviteCreateResponse(true, false, invite.getCode());
                })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new InviteCreateResponse(false, false, e.getMessage())))
                .onErrorReturn(new InviteCreateResponse(false, true, "server.error"));
    }

    // Record for response when creating an invite
    public record InviteCreateResponse(boolean success, boolean error, String data) {}

    // Record for invite create form
    public record InviteCreateForm(String token) {}

    @GetMapping(value = "/image", produces = "image/png")
    @CrossOrigin
    public ResponseEntity<InputStreamResource> getCodeImage(@RequestParam("code") String code) throws WriterException, IOException {

        // Get the qr code
        BufferedImage image = TwoFactorUtil.createQRCode(Configuration.constants.get("url") + "register?code=" + code);

        // Turn qr code into input stream
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        // Return new Input stream resource
        return ResponseEntity.ok()
                .body(new InputStreamResource(is));
    }

}
