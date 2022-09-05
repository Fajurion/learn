package com.fajurion.learn.repository.report;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ReportRepository extends ReactiveCrudRepository<Report, Integer> {

    @Query("select * from reports order by date limit :limit offset :offset")
    Flux<Report> sortReportsByDate(@Param("limit") int limit, @Param("offset") int offset);

}
