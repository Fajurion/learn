package com.fajurion.learn.controller.group.exams;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.groups.GroupRepository;
import com.fajurion.learn.repository.groups.member.MemberRepository;
import com.fajurion.learn.repository.tests.Exam;
import com.fajurion.learn.repository.tests.ExamRepository;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/group/exam")
public class ExamController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for getting group data
    private final GroupRepository groupRepository;

    // Repository for creating/editing exams
    private final ExamRepository examRepository;

    // Repository for getting member status
    private final MemberRepository memberRepository;

    @Autowired
    public ExamController(SessionService sessionService,
                          GroupRepository groupRepository,
                          ExamRepository examRepository,
                          MemberRepository memberRepository) {
        this.sessionService = sessionService;
        this.groupRepository = groupRepository;
        this.examRepository = examRepository;
        this.memberRepository = memberRepository;
    }

    @PostMapping("/create")
    @CrossOrigin
    public Mono<ExamCreateResponse> create(@RequestBody ExamCreateForm form) {

        // Check if form is valid
        if(form.token() == null || form.group() < 0 || form.name() == null || form.name().length() > 32) {
            return Mono.just(new ExamCreateResponse(false, false, "invalid"));
        }

        // Check if session token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Check if group exists and check if account is a member of the group
            return Mono.zip(groupRepository.findById(form.group()).hasElement(),
                    memberRepository.getMembersByAccountAndGroup(session.getAccount(), form.group()).hasElements());
        }).flatMap(tuple2 -> {

            if(!tuple2.getT1() || !tuple2.getT2()) {
                return Mono.error(new CustomException("not_found"));
            }

            // Create exam
            return examRepository.save(new Exam(form.name(), "", form.date(), form.group()));
        }).map(exam -> new ExamCreateResponse(true, false, "success"))

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new ExamCreateResponse(false, false, e.getMessage())))
                .onErrorReturn(new ExamCreateResponse(false, true, "server.error"));
    }

    // Form for creating an exam
    public record ExamCreateForm(String token, String name, int group, long date) {}

    // Response to creating an exam
    public record ExamCreateResponse(boolean success, boolean error, String message) {}

    @PostMapping("/board")
    @CrossOrigin
    public Mono<EditBoardResponse> board(@RequestBody EditBoardForm form) {

        // Check if form is valid
        if(form.token() == null || form.board() == null || form.board().length() > 500 || form.exam() < 0) {
            return Mono.just(new EditBoardResponse(false, false, "invalid"));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get exam
            return Mono.zip(examRepository.findById(form.exam()).onErrorReturn(new Exam("", "", -1, -1)),
                    Mono.just(session.getAccount()));
        }).flatMap(tuple -> {

            // Check if exam exists
            if(tuple.getT1().getGroupID() == -1) {
                return Mono.error(new CustomException("not_found"));
            }

            // Check if user is member of group
            return Mono.zip(memberRepository.getMembersByAccountAndGroup(tuple.getT2(), tuple.getT1().getGroupID()).hasElements(),
                    Mono.just(tuple.getT2()), Mono.just(tuple.getT1()));
        }).flatMap(tuple -> {

            if(!tuple.getT1()) {
                return Mono.error(new CustomException("no_member"));
            }

            // Edit board
            Exam exam = tuple.getT3();
            exam.setBoard(form.board());

            // Save exam
            return examRepository.save(exam);
        }).map(exam -> new EditBoardResponse(true, false, "success"))

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new EditBoardResponse(false, false, e.getMessage())))
                .onErrorReturn(new EditBoardResponse(false, true, "server.error"));

    }

    // Form for editing the board
    public record EditBoardForm(String token, String board, int exam) {}

    // Response to editing the board
    public record EditBoardResponse(boolean success, boolean error, String message) {}

}
