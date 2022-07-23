package com.fajurion.learn.controller.group;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.groups.Group;
import com.fajurion.learn.repository.groups.GroupRepository;
import com.fajurion.learn.repository.groups.GroupService;
import com.fajurion.learn.repository.groups.member.Member;
import com.fajurion.learn.repository.groups.member.MemberRepository;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/group")
public class GroupMemberController {

    // Service for checking and renewing token
    private final SessionService sessionService;

    // Repository for accessing group data
    private final GroupRepository groupRepository;

    // Repository for accessing member data
    private final MemberRepository memberRepository;

    @Autowired
    public GroupMemberController(SessionService sessionService, GroupRepository groupRepository, MemberRepository memberRepository) {
        this.sessionService = sessionService;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
    }


    @PostMapping("/join")
    @CrossOrigin
    public Mono<GroupStateResponse> join(@RequestBody GroupStateForm form) {

        // Check if form is valid
        if(form.token() == null || form.group() < 0) {
            return Mono.just(new GroupStateResponse(false, false, "empty"));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Check if the account is already in the group and check if group exists (and zip with account id)
            return Mono.zip(memberRepository.getMembersByAccountAndGroup(session.getAccount(), form.group()).hasElements(),
                    groupRepository.findById(form.group()).onErrorReturn(new Group("", "", -1)), Mono.just(session.getAccount()));
        }).flatMap(tuple -> {

            // Check if group exists
            if(tuple.getT2().getCreator() == -1) {
                return Mono.error(new CustomException("not_found"));
            }

            // Check if player is already a member
            if(tuple.getT1()) {
                return Mono.error(new CustomException("already_joined"));
            }

            // Save join status
            return memberRepository.save(new Member(form.group(), tuple.getT3(), System.currentTimeMillis()));
        }).map(member -> {

            // Return response
            return new GroupStateResponse(true, false, "success");
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new GroupStateResponse(false, false, e.getMessage())))
                .onErrorReturn(new GroupStateResponse(false, true, "server.error"));
    }

    @PostMapping("/leave")
    @CrossOrigin
    public Mono<GroupStateResponse> leave(@RequestBody GroupStateForm form) {

        // Check if form is valid
        if(form.token() == null || form.group() < 0) {
            return Mono.just(new GroupStateResponse(false, false, "empty"));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

                    if(session == null) {
                        return Mono.error(new CustomException("session.expired"));
                    }

                    // Check if the account is not in the group and check if group exists (and zip with account id)
                    return Mono.zip(memberRepository.getMembersByAccountAndGroup(session.getAccount(), form.group()).elementAt(0)
                                    .onErrorReturn(new Member(-1, -1, -1)),
                            groupRepository.findById(form.group()).onErrorReturn(new Group("", "", -1)), Mono.just(session.getAccount()));
                }).flatMap(tuple -> {

                    // Check if group exists
                    if(tuple.getT2().getCreator() == -1) {
                        return Mono.error(new CustomException("not_found"));
                    }

                    // Check if member is owner of the group
                    if(tuple.getT2().getCreator() == tuple.getT3()) {
                        return Mono.error(new CustomException("creator"));
                    }

                    // Check if member is not in the group
                    if(tuple.getT1().getAccount() == -1) {
                        return Mono.error(new CustomException("not_joined"));
                    }

                    // Delete member
                    return memberRepository.delete(tuple.getT1()).thenReturn(tuple.getT1());
                }).map(member -> {

                    // Return response
                    return new GroupStateResponse(true, false, "success");
                })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new GroupStateResponse(false, false, e.getMessage())))
                .onErrorReturn(new GroupStateResponse(false, true, "server.error"));
    }

    // Form for joining/leaving a group
    public record GroupStateForm(String token, int group) {}

    // Response for leaving/joining a group
    public record GroupStateResponse(boolean success, boolean error, String message) {}

}
