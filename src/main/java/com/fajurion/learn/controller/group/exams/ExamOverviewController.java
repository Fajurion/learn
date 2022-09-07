package com.fajurion.learn.controller.group.exams;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.tests.Exam;
import com.fajurion.learn.repository.tests.ExamRepository;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/group/exam")
public class ExamOverviewController {

    // Service to check sessions
    private final SessionService sessionService;

    // Repository for getting exams
    private final ExamRepository examRepository;

    @Autowired
    public ExamOverviewController(SessionService sessionService,
                                  ExamRepository examRepository) {
        this.sessionService = sessionService;
        this.examRepository = examRepository;
    }

    @PostMapping("/list")
    @CrossOrigin
    public Mono<ExamListResponse> list(@RequestBody ExamListForm form) {

        // Check if form is valid
        if(form.token() == null || form.offset() < 0 || form.group() < 0) {
            return Mono.just(new ExamListResponse(false, false, "invalid", null));
        }

        // Check if session token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Return sorted list of exams
            return examRepository.sortByDate(20, form.offset(), form.group()).collectList();
        }).map(list -> new ExamListResponse(true, false, "success", list))

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new ExamListResponse(false, false, e.getMessage(), null)))
                .onErrorReturn(new ExamListResponse(false, true, "server.error", null));
    }

    // Form for requesting a list of exams
    public record ExamListForm(String token, int group, int offset) {}

    // Response to requesting a list of exams
    public record ExamListResponse(boolean success, boolean error, String message, List<Exam> exams) {}

}
