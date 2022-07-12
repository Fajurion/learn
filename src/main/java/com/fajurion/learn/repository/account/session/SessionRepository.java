package com.fajurion.learn.repository.account.session;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SessionRepository extends ReactiveCrudRepository<Session, Integer> {

    @Query("select * from sessions where account = :account")
    Flux<Session> getSessionsByAccount(@Param("account") int account);

    Mono<Session> getSessionByToken(@Param("token") String token);

}
