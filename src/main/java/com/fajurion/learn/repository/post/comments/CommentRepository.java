package com.fajurion.learn.repository.post.comments;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CommentRepository extends ReactiveCrudRepository<Comment, Integer> {

    // Deletion methods
    Mono<Void> deleteAllByPost(@Param("post") int post);
    Mono<Void> deleteAllByTopic(@Param("topic") int topic);

}
