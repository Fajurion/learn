package com.fajurion.learn.repository.tests;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends ReactiveCrudRepository<Exam, Integer> {



}
