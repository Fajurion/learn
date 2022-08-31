package com.fajurion.learn.controller.start;

import com.fajurion.learn.repository.account.Account;
import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.groups.member.MemberService;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api")
public class StartAppController {

    // Service for getting member data
    private final MemberService memberService;

    // Service for checking session token
    private final SessionService sessionService;

    // Repository for getting account data
    private final AccountRepository accountRepository;

    @Autowired
    public StartAppController(MemberService memberService,
                              SessionService sessionService,
                              AccountRepository accountRepository) {
        this.memberService = memberService;
        this.sessionService = sessionService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/start")
    @CrossOrigin
    public Mono<StartPageResponse> start(@RequestBody StartPageForm form) {

        // Check if form is valid
        if(form.token() == null) {
            return Mono.just(new StartPageResponse(false, false, "empty", null, null, null));
        }

        // Check session token (and renew if valid)
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get groups and zip with tests TODO: Add class tests to start
            return Mono.zip(accountRepository.findById(session.getAccount()).onErrorReturn(new Account("", "", "", "", "", -1)),
                    memberService.getGroups(session.getAccount(), 1000));
        }).map(tuple2 -> {

            // Check if account doesn't exist anymore
            if(tuple2.getT1().getInvitor() == -1) {
                return new StartPageResponse(false, true, "account.deleted", null, null, null);
            }

            // Return response
            return new StartPageResponse(true, false, "success", tuple2.getT2(), null, tuple2.getT1());
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new StartPageResponse(false, false, e.getMessage(), null, null, null)))
                .onErrorReturn(new StartPageResponse(false, true, "server.error", null, null, null));
    }


    // Form for getting start page
    public record StartPageForm(String token) {}

    // Response entity for groups
    public record GroupEntity(String name, int id) {}

    // Response entity for tests
    public record TestEntity(String name, int id) {}

    // Response for getting start page endpoint
    public record StartPageResponse(boolean success, boolean error, String message,
                                    List<GroupEntity> groups, List<TestEntity> tests, Account account) {}

}
