package com.fajurion.learn.repository.topic;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface TopicRepository extends ReactiveCrudRepository<Topic, Integer> {

    @Query("select * from topics where parent = :parent order by id")
    Flux<Topic> getTopicsByParent(@Param("parent") int parent);

    Mono<Long> countTopicsByParent(@Param("parent") int parent);

}
