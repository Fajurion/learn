package com.fajurion.learn.controller.group;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.groups.Group;
import com.fajurion.learn.repository.groups.GroupRepository;
import com.fajurion.learn.repository.groups.GroupResponse;
import com.fajurion.learn.repository.groups.GroupService;
import com.fajurion.learn.repository.groups.member.MemberRepository;
import com.fajurion.learn.repository.groups.member.MemberResponse;
import com.fajurion.learn.repository.groups.member.MemberService;
import com.fajurion.learn.repository.tests.Exam;
import com.fajurion.learn.repository.tests.ExamRepository;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
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

    // Service for getting better group data
    private final GroupService groupService;

    // Service for getting better member data
    private final MemberService memberService;

    // Repository for getting exams
    private final ExamRepository examRepository;

    @Autowired
    public GroupOverviewController(SessionService sessionService,
                                   GroupRepository groupRepository,
                                   MemberRepository memberRepository,
                                   GroupService groupService,
                                   MemberService memberService,
                                   ExamRepository examRepository) {
        this.sessionService = sessionService;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.groupService = groupService;
        this.memberService = memberService;
        this.examRepository = examRepository;
    }

    @PostMapping("/get")
    @CrossOrigin
    public Mono<GroupInfoResponse> get(@RequestBody GroupInfoForm form) {

        // Check if form is valid
        if(form.token() == null) {
            return Mono.just(new GroupInfoResponse(false, true, "empty", "", "", 0,
                    false, false, new ArrayList<>(), null));
        }

        // Check if token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

                    if(session == null) {
                        return Mono.error(new CustomException("session.expired"));
                    }

                    // Check if group exists
                    return Mono.zip(groupRepository.findById(form.group()).onErrorReturn(new Group("", "", -1)), Mono.just(session.getAccount()));
                }).flatMap(tuple2 -> {

                    if(tuple2.getT1().getCreator() == -1) {
                        return Mono.error(new CustomException("not_found"));
                    }

                    // Get the information about the group
                    return Mono.zip(memberRepository.countByGroup(tuple2.getT1().getId()), Mono.just(tuple2.getT1()), Mono.just(tuple2.getT2()),
                            memberRepository.getMembersByAccountAndGroup(tuple2.getT2(), tuple2.getT1().getId()).hasElements(),
                            memberService.getMembers(form.group(), 10).onErrorReturn(new ArrayList<>()),
                            examRepository.sortByDate(20, 0, form.group()).collectList());
                }).map(tuple -> {

                    // Return response
                    return new GroupInfoResponse(true, false, "success",
                            tuple.getT2().getName(), tuple.getT2().getDescription(),
                            tuple.getT1().intValue(), tuple.getT4(), tuple.getT3() == tuple.getT2().getCreator(), tuple.getT5(), tuple.getT6());
                })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new GroupInfoResponse(false, false, e.getMessage(), "", "",
                        0, false, false, new ArrayList<>(), null)))
                .onErrorReturn(new GroupInfoResponse(false, true, "server.error", "", "",
                        0, false, false, new ArrayList<>(), null));
    }

    // Form for getting group info
    public record GroupInfoForm(String token, int group) {}

    // Response to group info endpoint
    public record GroupInfoResponse(boolean success, boolean error, String message, String name, String description,
                                    int memberCount, boolean member, boolean creator, List<MemberResponse> members,
                                    List<Exam> exams) {}

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

            // Get group responses
            return groupService.getResponseList(form.limit(), form.offset());
        }).map(list -> {

            // Return response
            return new GroupListResponse(true, false, "success", list);
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new GroupListResponse(false, false, e.getMessage(), new ArrayList<>())))
                .onErrorReturn(new GroupListResponse(false, true, "server.error", new ArrayList<>()));
    }

    // Form for getting a group list
    public record GroupListForm(String token, int limit, int offset) {}

    // Response to group info/search endpoint
    public record GroupListResponse(boolean success, boolean error, String message, List<GroupResponse> groups) {}

    @PostMapping("/search")
    @CrossOrigin
    public Mono<GroupListResponse> search(@RequestBody GroupSearchForm form) {

        // Check if form is valid
        if(form.token() == null || form.offset() < 0 || form.limit() < 1 || form.limit() > 20 || form.name() == null) {
            return Mono.just(new GroupListResponse(false, false, "empty", new ArrayList<>()));
        }

        // Check if token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Search groups
            return groupService.searchResponseList(form.name(), form.limit(), form.offset());
        }).map(groups -> {

            // Return response
            return new GroupListResponse(true, false, "success", groups);
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new GroupListResponse(true, false, e.getMessage(), new ArrayList<>())))
                .onErrorReturn(new GroupListResponse(false, true, "server.error", new ArrayList<>()));
    }

    // Form for searching groups
    public record GroupSearchForm(String token, String name, int limit, int offset) {}

}
