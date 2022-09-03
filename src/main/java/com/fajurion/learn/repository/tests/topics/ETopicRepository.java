package com.fajurion.learn.repository.tests.topics;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ETopicRepository extends ReactiveCrudRepository<ETopic, Integer> {
}
