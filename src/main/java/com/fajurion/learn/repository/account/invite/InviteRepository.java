package com.fajurion.learn.repository.account.invite;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface InviteRepository extends ReactiveCrudRepository<Invite, Integer> {

    Flux<Invite> getInvitesByCode(@Param("code") String code);

    @Query("select * from invites order by id limit :limit")
    Flux<Invite> getInvitesSorted(@Param("limit") int limit);

}
