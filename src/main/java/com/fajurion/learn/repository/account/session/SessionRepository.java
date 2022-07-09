package com.fajurion.learn.repository.account.session;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SessionRepository extends ReactiveCrudRepository<Session, String> {

    Flux<Session> getSessionsById(@Param("id") int id);

    Mono<Long> countSessionsById(@Param("id") int id);

}
