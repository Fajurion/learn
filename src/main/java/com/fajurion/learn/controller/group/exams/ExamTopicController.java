package com.fajurion.learn.controller.group.exams;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.groups.member.MemberRepository;
import com.fajurion.learn.repository.tests.ExamRepository;
import com.fajurion.learn.repository.tests.topics.ETopic;
import com.fajurion.learn.repository.tests.topics.ETopicRepository;
import com.fajurion.learn.repository.tests.topics.ETopicResponse;
import com.fajurion.learn.repository.topic.TopicRepository;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/group/exam/topic")
public class ExamTopicController {

    // Repository for adding/removing/listing exam topics
    private final ETopicRepository eTopicRepository;

    // Repository for getting topics
    private final TopicRepository topicRepository;

    // Service to check session tokens
    private final SessionService sessionService;

    // Repository to get exam data
    private final ExamRepository examRepository;

    // Repository for getting member data
    private final MemberRepository memberRepository;

    @Autowired
    public ExamTopicController(SessionService sessionService,
                               ETopicRepository eTopicRepository,
                               TopicRepository topicRepository,
                               ExamRepository examRepository,
                               MemberRepository memberRepository) {
        this.sessionService = sessionService;
        this.eTopicRepository = eTopicRepository;
        this.topicRepository = topicRepository;
        this.examRepository = examRepository;
        this.memberRepository = memberRepository;
    }

    @PostMapping("/list")
    @CrossOrigin
    public Mono<ExamListResponse> list(@RequestBody ExamListForm form) {

        // Check if form is valid
        if(form.token() == null || form.exam() < 0 || form.offset() < 0) {
            return Mono.just(new ExamListResponse(false, false, "invalid", null));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get exam topics
            return eTopicRepository.sortTopics(form.exam(), 50, form.offset()).flatMap(topic -> {

                // Turn into topic
                return Mono.zip(topicRepository.findById(topic.getTopic()), Mono.just(topic.getId()));
            }).map(tuple2 -> new ETopicResponse(tuple2.getT1(), tuple2.getT2())).collectList();
        }).map(list -> new ExamListResponse(true, false, "success", list))

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new ExamListResponse(false, false, e.getMessage(), null)))
                .onErrorReturn(new ExamListResponse(false, true, "server.error", null));
    }

    // Form for requesting a list of exam topics
    public record ExamListForm(String token, int exam, int offset) {}

    // Response to requesting a list of exam topics
    public record ExamListResponse(boolean success, boolean error, String message, List<ETopicResponse> topics) {}

    @PostMapping("/add")
    @CrossOrigin
    public Mono<TopicModifyResponse> add(@RequestBody TopicModifyForm form) {

        // Check if form is valid
        if(form.token() == null || form.exam() < 0 || form.group() < 0 || form.topic() < 0) {
            return Mono.just(new TopicModifyResponse(false, false, "invalid"));
        }

        // Check if session token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Check if user is member of the group, check if exam exists and check if topic exists
            return Mono.zip(memberRepository.getMembersByAccountAndGroup(session.getAccount(), form.group()).hasElements(),
                    Mono.just(session.getAccount()), examRepository.findById(form.exam()).hasElement(),
                    eTopicRepository.getETopicsByTopicAndTest(form.exam(), form.topic()).hasElements(),
                    topicRepository.findById(form.topic()).hasElement());
        }).flatMap(tuple2 -> {

            // Check if exam and group exist
            if(!tuple2.getT1() || !tuple2.getT3()) {
                return Mono.error(new CustomException("not_found"));
            }

            // Check if topic exists
            if(!tuple2.getT5()) {
                return Mono.error(new CustomException("topic.not_found"));
            }

            // Check if topic is already added
            if(tuple2.getT4()) {
                return Mono.error(new CustomException("already.added"));
            }

            // Add topic
            return eTopicRepository.save(new ETopic(form.exam(), form.topic()));
        }).map(topic -> new TopicModifyResponse(true, false, "success"))

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new TopicModifyResponse(false, false, e.getMessage())))
                .onErrorReturn(new TopicModifyResponse(false, true, "server.error"));
    }

    // Form for requesting to add/remove an exam topic
    public record TopicModifyForm(String token, int exam, int group, int topic) {}

    // Response to requesting to add/remove a topic
    public record TopicModifyResponse(boolean success, boolean error, String message) {}

    @PostMapping("/remove")
    @CrossOrigin
    public Mono<TopicModifyResponse> remove(@RequestBody TopicModifyForm form) {

        // Check if form is valid
        if(form.token() == null || form.exam() < 0 || form.group() < 0 || form.topic() < 0) {
            return Mono.just(new TopicModifyResponse(false, false, "invalid"));
        }

        // Check if session token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Check if user is member of the group, check if exam exists and check if topic exists
            return Mono.zip(memberRepository.getMembersByAccountAndGroup(session.getAccount(), form.group()).hasElements(),
                    Mono.just(session.getAccount()), examRepository.findById(form.exam()).hasElement(),
                    eTopicRepository.getETopicsByTopicAndTest(form.exam(), form.topic()).elementAt(0).onErrorReturn(new ETopic(-1, -1)),
                    topicRepository.findById(form.topic()).hasElement());
        }).flatMap(tuple2 -> {

            // Check if exam and group exist
            if(!tuple2.getT1() || !tuple2.getT3()) {
                return Mono.error(new CustomException("not_found"));
            }

            // Check if topic exists
            if(!tuple2.getT5()) {
                return Mono.error(new CustomException("topic.not_found"));
            }

            // Check if topic is not added
            if(tuple2.getT4().getTopic() == -1) {
                return Mono.error(new CustomException("not.added"));
            }

            // Remove topic from the list
            return eTopicRepository.delete(tuple2.getT4()).thenReturn(tuple2.getT4());
        }).map(topic -> new TopicModifyResponse(true, false, "success"))

        // Error handling
        .onErrorResume(CustomException.class, e -> Mono.just(new TopicModifyResponse(false, false, e.getMessage())))
        .onErrorReturn(new TopicModifyResponse(false, true, "server.error"));
    }

}
