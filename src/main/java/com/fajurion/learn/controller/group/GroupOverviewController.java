package com.fajurion.learn.controller.group;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.groups.Group;
import com.fajurion.learn.repository.groups.GroupRepository;
import com.fajurion.learn.repository.groups.member.MemberRepository;
import com.fajurion.learn.util.CustomException;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/api/group")
@RestController
public class GroupOverviewController {

    // Service for checking and renewing token
    private final SessionService sessionService;

    // Repository for accessing group data
    private final GroupRepository groupRepository;

    // Repository for accessing member data
    private final MemberRepository memberRepository;

    public GroupOverviewController(SessionService sessionService, GroupRepository groupRepository, MemberRepository memberRepository) {
        this.sessionService = sessionService;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
    }

    @PostMapping("/get")
    @CrossOrigin
    public Mono<GroupInfoResponse> get(@RequestBody GroupInfoForm form) {

        // Check if form is valid
        if(form.token() == null) {
            return Mono.just(new GroupInfoResponse(false, true, "empty", "", "", 0));
        }

        // Check if token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

                    if(session == null) {
                        return Mono.error(new CustomException("session.expired"));
                    }

                    // Check if group exists
                    return groupRepository.findById(form.group());
                }).flatMap(group -> {

                    if(group == null) {
                        return Mono.error(new CustomException("not_found"));
                    }

                    // Get the information about the group
                    return Mono.zip(memberRepository.countByGroup(group.getId()), Mono.just(group));
                }).map(tuple -> {

                    // Return response
                    return new GroupInfoResponse(true, false, "success",
                            tuple.getT2().getName(), tuple.getT2().getDescription(),
                            tuple.getT1().intValue());
                })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new GroupInfoResponse(false, false, e.getMessage(), "", "", 0)))
                .onErrorReturn(new GroupInfoResponse(false, true, "server.error", "", "", 0));
    }

    // Form for getting group info
    public record GroupInfoForm(String token, int group) {}

    // Response to group info endpoint
    public record GroupInfoResponse(boolean success, boolean error, String message, String name, String description, int memberCount) {}

    @PostMapping("/list")
    @CrossOrigin
    public Mono<GroupListResponse> list(@RequestBody GroupListForm form) {

        // Check if form is valid
        if(form.token() == null || form.offset() < 0 || form.limit() < 1 || form.limit() > 20) {
            return Mono.just(new GroupListResponse(false, false, "empty", new ArrayList<>()));
        }

        // Check if token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get groups
            return groupRepository.getGroupsBy(form.limit(), form.offset()).collectList();
        }).map(list -> {
            ArrayList<String> nameList = new ArrayList<>();

            // Convert group list to name list
            for(Group group : list) {
                nameList.add(group.getName());
            }

            // Return response
            return new GroupListResponse(true, false, "success", nameList);
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new GroupListResponse(false, false, e.getMessage(), new ArrayList<>())))
                .onErrorReturn(new GroupListResponse(false, true, "server.error", new ArrayList<>()));
    }

    // Form for getting a group list
    public record GroupListForm(String token, int group, int limit, int offset) {}

    // Response to group info endpoint
    public record GroupListResponse(boolean success, boolean error, String message, List<String> groups) {}

}
