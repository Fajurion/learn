package com.fajurion.learn.repository.groups.member;

import com.fajurion.learn.controller.start.StartAppController;
import com.fajurion.learn.repository.account.Account;
import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.groups.Group;
import com.fajurion.learn.repository.groups.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.List;

@Service
public class MemberService {

    // Repository for getting account data by id
    private final AccountRepository accountRepository;

    // Repository for getting member data
    private final MemberRepository memberRepository;

    // Repository for getting group data
    private final GroupRepository groupRepository;

    @Autowired
    public MemberService(AccountRepository accountRepository,
                         MemberRepository memberRepository,
                         GroupRepository groupRepository) {
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.groupRepository = groupRepository;
    }

    public Mono<List<MemberResponse>> getMembers(int group, int limit) {
        return memberRepository.getMembersByGroupLimit(group, limit)
                .flatMap(member ->  Mono.zip(accountRepository.findById(member.getAccount()).onErrorReturn(new Account("", "", "", "", "", -1)), Mono.just(member)))
                .collectList().map(list -> {

                    // Convert list to member response list
                    ArrayList<MemberResponse> responses = new ArrayList<>();

                    for(Tuple2<Account, Member> tuple : list) {
                        responses.add(new MemberResponse(tuple.getT1().getUsername(), tuple.getT2().getId()));
                    }

                    // Return converted list
                    return responses;
                });
    }

    public Mono<List<StartAppController.GroupEntity>> getGroups(int account, int limit) {
        return memberRepository.getMembersByAccountLimit(account, limit)
                .flatMap(member -> groupRepository.findById(member.getGroup()).onErrorReturn(new Group("", "", -1)))
                .collectList().map(list -> {

                    // Convert list to group entity list
                    ArrayList<StartAppController.GroupEntity> entities = new ArrayList<>();

                    for(Group group : list) {
                        entities.add(new StartAppController.GroupEntity(group.getName(), group.getId()));
                    }

                    return entities;
                });
    }

}
