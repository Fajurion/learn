package com.fajurion.learn.controller.group;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.groups.Group;
import com.fajurion.learn.repository.groups.GroupRepository;
import com.fajurion.learn.repository.groups.member.Member;
import com.fajurion.learn.repository.groups.member.MemberRepository;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RequestMapping("/api/group")
@RestController
public class GroupController {

    // Service for checking and renewing token
    private final SessionService sessionService;

    // Repository for accessing group data
    private final GroupRepository groupRepository;

    // Repository for accessing member data
    private final MemberRepository memberRepository;

    @Autowired
    public GroupController(SessionService sessionService, GroupRepository groupRepository, MemberRepository memberRepository) {
        this.sessionService = sessionService;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
    }

    @PostMapping("/create")
    @CrossOrigin
    public Mono<GroupCreateResponse> create(@RequestBody GroupCreateForm form) {

        // Check if the name fits the requirements
        if(form.name() == null || form.token() == null) {
            return Mono.just(new GroupCreateResponse(false, true, "empty", -1));
        }

        if(form.name().length() < 3) {
            return Mono.just(new GroupCreateResponse(false, false, "name.too_short", -1));
        }

        if(form.name().length() > 20) {
            return Mono.just(new GroupCreateResponse(false, false, "name.too_long", -1));
        }

        // Check if token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Check if group name is taken and zip with the user ID
            return Mono.zip(groupRepository.getGroupByNameIgnoreCase(form.name()).hasElement(), Mono.just(session.getAccount()));
        }).flatMap(tuple2 -> {

            if(tuple2.getT1()) {
                return Mono.error(new CustomException("name.taken"));
            }

            // Create group and zip with the user ID
            return Mono.zip(groupRepository.save(new Group(form.name(), "", tuple2.getT2())), Mono.just(tuple2.getT2()));
        }).flatMap(tuple2 -> {

            // Add user to the group and zip with the group ID
            return Mono.zip(memberRepository.save(new Member(tuple2.getT1().getId(), tuple2.getT2(), System.currentTimeMillis())), Mono.just(tuple2.getT1().getId()));
        }).map(tuple2 -> {

            // Return response
            return new GroupCreateResponse(true, false, "success", tuple2.getT2());
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new GroupCreateResponse(false, false, e.getMessage(), -1)))
                .onErrorReturn(new GroupCreateResponse(false, true, "server.error", -1));
    }

    // Form for creating a group
    public record GroupCreateForm(String token, String name) {}

    // Response for the create group endpoint
    public record GroupCreateResponse(boolean success, boolean error, String message, int groupID) {}

    @PostMapping("/edit")
    @CrossOrigin
    public Mono<DescriptionEditResponse> editDescription(@RequestBody DescriptionEditForm form) {

        // Check if description fits requirements
        if(form.description() == null || form.token() == null) {
            return Mono.just(new DescriptionEditResponse(false, true, "empty"));
        }

        if(form.description().length() > 500) {
            return Mono.just(new DescriptionEditResponse(false, false, "description.too_long"));
        }

        // Check if token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get the group and zip with user id
            return Mono.zip(groupRepository.findById(form.group()), Mono.just(session.getAccount()));
        }).flatMap(group -> {

            if(group == null) {
                return Mono.error(new CustomException("not_found"));
            }

                    System.out.println(group.getT1().getCreator() + " | " + group.getT2());
            // Check if the user is the creator of the group
            if(group.getT1().getCreator() != group.getT2()) {
                return Mono.error(new CustomException("no_permission"));
            }

            // Edit the description
            group.getT1().setDescription(form.description());
            return groupRepository.save(group.getT1());
        }).map(group -> {

            // Return response
            return new DescriptionEditResponse(true, false, "success");
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new DescriptionEditResponse(false, false, e.getMessage())))
                .onErrorReturn(new DescriptionEditResponse(false, true, "server.error"));
    }

    // Form for editing description
    public record DescriptionEditForm(String token, int group, String description) {}

    // Response for editing group endpoint
    public record DescriptionEditResponse(boolean success, boolean error, String message) {}

}
