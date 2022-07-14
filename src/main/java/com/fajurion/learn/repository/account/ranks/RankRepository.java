package com.fajurion.learn.repository.account.ranks;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface RankRepository extends ReactiveCrudRepository<Rank, Integer> {

    Mono<Rank> getRankByName(@Param("name") String name);

}
