package com.fajurion.learn.repository.topic;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TopicRepository extends ReactiveCrudRepository<Topic, Integer> {

    Flux<Topic> getTopicsByParent(@Param("parent") int parent);

}
