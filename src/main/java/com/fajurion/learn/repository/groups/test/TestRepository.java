package com.fajurion.learn.repository.groups.test;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestRepository extends ReactiveCrudRepository<Test, Integer> {
}
