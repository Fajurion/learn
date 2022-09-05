package com.fajurion.learn.repository.tasks.likes;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TLikeRepository extends ReactiveCrudRepository<TLike, Integer> {

    Mono<TLike> getLikeByTaskAndAccount(@Param("task") int task, @Param("account") int account);

    Mono<TLike> deleteAllByTask(@Param("task") int task);

    Mono<Void> deleteByTaskAndAccount(@Param("task") int task, @Param("account") int account);

}
