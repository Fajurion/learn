package com.fajurion.learn.repository.tests;

import com.fajurion.learn.repository.groups.member.Member;
import com.fajurion.learn.repository.groups.member.MemberRepository;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExamService {

    // Repository for getting members
    private final MemberRepository memberRepository;

    // Repository for getting exam data
    private final ExamRepository examRepository;

    @Autowired
    public ExamService(MemberRepository memberRepository,
                       ExamRepository examRepository) {
        this.memberRepository = memberRepository;
        this.examRepository = examRepository;
    }

    public Mono<List<Exam>> getExams(int account, int limit, int offset) {

        // Get members
        return memberRepository.getMembersByAccountLimit(account, 1000).collectList().flatMap(list -> {

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
            return examRepository.getExamsFromGroups(limit, offset, ids).collectList();
        });
    }

}
