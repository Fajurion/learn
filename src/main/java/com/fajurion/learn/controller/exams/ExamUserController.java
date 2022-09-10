package com.fajurion.learn.controller.exams;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.groups.member.Member;
import com.fajurion.learn.repository.groups.member.MemberRepository;
import com.fajurion.learn.repository.tests.Exam;
import com.fajurion.learn.repository.tests.ExamRepository;
import com.fajurion.learn.util.CustomException;
import com.fajurion.learn.util.QueryUtil;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/exam")
public class ExamUserController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for getting members of groups
    private final MemberRepository memberRepository;

    // Repository for getting exam data
    private final ExamRepository examRepository;

    public ExamUserController(SessionService sessionService,
                              ExamRepository examRepository,
                              MemberRepository memberRepository) {
        this.sessionService = sessionService;
        this.examRepository = examRepository;
        this.memberRepository = memberRepository;
    }

    @PostMapping("/list")
    @CrossOrigin
    public Mono<ExamListResponse> list(@RequestBody ExamListForm form) {

        // Check if form is valid
        if(form.token() == null || form.offset() < 0) {
            return Mono.just(new ExamListResponse(false, false, "invalid", null));
        }

        // Check if session token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get groups
            return memberRepository.getMembersByAccountLimit(session.getAccount(), 1000).collectList();
        }).flatMap(list -> {

            // Check if user is in a group
            if(list.isEmpty()) {
                return Mono.error(new CustomException("not_found"));
            }

            // Build ids for query
            ArrayList<Integer> ids = new ArrayList<>();

            for(Member member : list) {
                ids.add(member.getGroup());
            }

            // Get exams
            return examRepository.getExamsFromGroups(20, form.offset(), ids).collectList().onErrorResume(e -> {
                e.printStackTrace();
                return Mono.just(new ArrayList<>());
            });
        }).map(list -> {

            // Return response
            return new ExamListResponse(true, false, "success", list);
        })

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new ExamListResponse(false, false, e.getMessage(), null)))
                .onErrorResume(e ->  {
                    System.out.println(e.getMessage());
                    return Mono.just(new ExamListResponse(false, true, "server.error", null));
                })
                .onErrorReturn(new ExamListResponse(false, true, "server.error", null));
    }

    // Form for requesting a list of exams
    public record ExamListForm(String token, int offset) {}

    // Response to requesting a list of exams
    public record ExamListResponse(boolean success, boolean error, String message, List<Exam> exams) {}

}
