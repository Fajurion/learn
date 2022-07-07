package com.fajurion.learn.repository.post.likes;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends ReactiveCrudRepository<Like, Integer> {



}
