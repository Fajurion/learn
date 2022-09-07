package com.fajurion.learn.controller.account.invite;

import com.fajurion.learn.repository.account.invite.Invite;
import com.fajurion.learn.repository.account.invite.InviteRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/account/invite")
public class InviteListController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for modifying invites
    private final InviteRepository inviteRepository;

    @Autowired
    public InviteListController(SessionService sessionService,
                                InviteRepository inviteRepository) {
        this.sessionService = sessionService;
        this.inviteRepository = inviteRepository;
    }

    @PostMapping("/list")
    @CrossOrigin
    public Mono<InviteListResponse> list(@RequestBody InviteListForm form) {

        // Check if form is valid
        if(form.token() == null) {
            return Mono.just(new InviteListResponse(false, false, "invalid", null));
        }

        // Run normal permission flow
        return sessionService.checkPermissionAndRefresh(form.token(), "view.admin.panel").flatMap(permission -> {

            if(!permission) {
                return Mono.error(new CustomException("no_permission"));
            }

            // Get invites
            return inviteRepository.getInvitesSorted(100).collectList();
        }).map(invites -> new InviteListResponse(true, false, "success", invites))

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new InviteListResponse(false, false, e.getMessage(), null)))
                .onErrorReturn(new InviteListResponse(false, true, "server.error", null));
    }

    // Form for requesting invite list
    public record InviteListForm(String token) {}

    // Response to requesting a list of invites
    public record InviteListResponse(boolean success, boolean error, String message, List<Invite> invites) {}

    @PostMapping("/delete")
    @CrossOrigin
    public Mono<InviteDeleteResponse> delete(@RequestBody InviteDeleteForm form) {

        // Check if form is valid
        if(form.token() == null || form.inviteID < 0) {
            return Mono.just(new InviteDeleteResponse(false, false, "invalid"));
        }

        // Check session and permission
        return sessionService.checkPermissionAndRefresh(form.token(), "view.admin.panel").flatMap(permission -> {

            if(!permission) {
                return Mono.error(new CustomException("no_permission"));
            }

            // Delete invite
            return inviteRepository.deleteById(form.inviteID).thenReturn("d");
        }).map(s -> new InviteDeleteResponse(true, false, "success"))

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new InviteDeleteResponse(false, false, e.getMessage())))
                .onErrorReturn(new InviteDeleteResponse(false, true, "server.error"));
    }

    // Form for requesting invite list
    public record InviteDeleteForm(String token, int inviteID) {}

    // Response to requesting a list of invites
    public record InviteDeleteResponse(boolean success, boolean error, String message) {}

}
