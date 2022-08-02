package com.fajurion.learn.repository.tasks.reports;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TReportRepository extends ReactiveCrudRepository<TReport, Integer> {



}
