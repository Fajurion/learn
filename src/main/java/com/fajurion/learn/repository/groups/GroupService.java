package com.fajurion.learn.repository.groups;

import com.fajurion.learn.repository.groups.member.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.ArrayList;

@Service
public class GroupService {

    // Repository to get members of a group
    private final MemberRepository memberRepository;

    // Repository for getting data about groups
    private final GroupRepository groupRepository;

    @Autowired
    public GroupService(MemberRepository memberRepository, GroupRepository groupRepository) {
        this.memberRepository = memberRepository;
        this.groupRepository = groupRepository;
    }

    public Mono<ArrayList<GroupResponse>> getResponseList(int limit, int offset) {

        // Get all groups with limit and offset
        return groupRepository.getGroupsBy(limit, offset)
                .flatMap(group -> Mono.zip(memberRepository.countByGroup(group.getId()), Mono.just(group)))
                .collectList().map(list -> {
                    ArrayList<GroupResponse> responses = new ArrayList<>();

                    // Turn tuple2 into response
                    for(Tuple2<Long, Group> tuple2 : list) {
                        responses.add(new GroupResponse(tuple2.getT2(), tuple2.getT1().intValue()));
                    }

                    return responses;
                });
    }
}
