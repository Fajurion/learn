package com.fajurion.learn.repository.groups.member;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MemberRepository extends ReactiveCrudRepository<Member, Integer> {

    Mono<Long> countByGroup(@Param("group") int group);

    Flux<Member> getMembersByAccountAndGroup(@Param("account") int account, @Param("group") int group);

    @Query("select * from members where class = :group order by id limit :limit")
    Flux<Member> getMembersByGroupLimit(@Param("group") int group, @Param("limit") int limit);

    @Query("select * from members where account = :account order by id limit :limit")
    Flux<Member> getMembersByAccountLimit(@Param("account") int account, @Param("limit") int limit);

}
