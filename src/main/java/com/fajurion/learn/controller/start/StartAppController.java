package com.fajurion.learn.controller.start;

import com.beust.ah.A;
import com.fajurion.learn.repository.account.Account;
import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.ranks.Rank;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.groups.member.MemberService;
import com.fajurion.learn.util.Configuration;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/api")
public class StartAppController {

    // Service for getting member data
    private final MemberService memberService;

    // Service for checking session token
    private final SessionService sessionService;

    // Repository for getting account data
    private final AccountRepository accountRepository;

    // Repository for getting rank data
    private final RankRepository rankRepository;

    @Autowired
    public StartAppController(MemberService memberService,
                              SessionService sessionService,
                              AccountRepository accountRepository,
                              RankRepository rankRepository) {
        this.memberService = memberService;
        this.sessionService = sessionService;
        this.accountRepository = accountRepository;
        this.rankRepository = rankRepository;
    }

    @PostMapping("/start")
    @CrossOrigin
    public Mono<StartPageResponse> start(@RequestBody StartPageForm form) {

        // Check if form is valid
        if(form.token() == null) {
            return Mono.just(new StartPageResponse(false, false, "empty", null, null, null, null));
        }

        // Check session token (and renew if valid)
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get groups and zip with tests TODO: Add class tests to start
            return Mono.zip(accountRepository.findById(session.getAccount()).onErrorReturn(new Account("", "", "", "", "", -1)),
                    memberService.getGroups(session.getAccount(), 1000));
        }).flatMap(tuple -> Mono.zip(Mono.just(tuple), rankRepository.getRankByName(tuple.getT1().getRank()).onErrorReturn(new Rank("", -1)))).map(tuple2 -> {

            // Check for error
            if(tuple2.getT2().getLevel() == -1) {
                return new StartPageResponse(false, true, "server.error", null, null, null, null);
            }

            // Check if account doesn't exist anymore
            if(tuple2.getT1().getT1().getInvitor() == -1) {
                return new StartPageResponse(false, true, "account.deleted", null, null, null, null);
            }

            // Return permissions
            ArrayList<String> permissions = new ArrayList<>();
            for(Map.Entry<String, Integer> entry : Configuration.permissions.entrySet()) {
                if(entry.getValue() > tuple2.getT2().getLevel()) continue;
                permissions.add(entry.getKey());
            }

            // Return response
            return new StartPageResponse(true, false, "success", tuple2.getT1().getT2(), null, tuple2.getT1().getT1(), permissions);
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new StartPageResponse(false, false, e.getMessage(), null, null, null, null)))
                .onErrorReturn(new StartPageResponse(false, true, "server.error", null, null, null, null));
    }


    // Form for getting start page
    public record StartPageForm(String token) {}

    // Response entity for groups
    public record GroupEntity(String name, int id) {}

    // Response entity for tests
    public record TestEntity(String name, int id) {}

    // Response for getting start page endpoint
    public record StartPageResponse(boolean success, boolean error, String message,
                                    List<GroupEntity> groups, List<TestEntity> tests, Account account,
                                    Collection<String> permissions) {}

}
