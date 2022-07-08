package com.fajurion.learn.repository.topic;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Repository
public interface TopicRepository extends ReactiveCrudRepository<Topic, Integer> {

    Mono<ArrayList<Topic>> getTopicsByParent(@Param("parent") int parent);

    Mono<Long> countTopicsByParent(@Param("parent") int parent);

}
