package com.fajurion.learn.repository.tasks.likes;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TLikeRepository extends ReactiveCrudRepository<TLike, Integer> {
}
