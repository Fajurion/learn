package com.fajurion.learn.repository.account.session;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface SessionRepository extends ReactiveCrudRepository<Session, String> {

    Flux<Session> getSessionsById(@Param("id") int id);

}
