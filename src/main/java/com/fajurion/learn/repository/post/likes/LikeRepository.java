package com.fajurion.learn.repository.post.likes;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface LikeRepository extends ReactiveCrudRepository<Like, Integer> {

    Mono<Like> getLikeByPostAndAccount(@Param("post") int post,  @Param("account") int account);

}
