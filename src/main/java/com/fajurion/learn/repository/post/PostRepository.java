package com.fajurion.learn.repository.post;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface PostRepository extends ReactiveCrudRepository<Post, Integer> {

    // Deletion method
    Mono<Void> deleteAllByTopic(@Param("topic") int topic);

}
