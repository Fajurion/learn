package com.fajurion.learn.repository.groups.member;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface MemberRepository extends ReactiveCrudRepository<Member, Integer> {

    Mono<Long> countByGroup(@Param("group") int group);

}
