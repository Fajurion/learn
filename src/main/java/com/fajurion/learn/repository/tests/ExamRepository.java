package com.fajurion.learn.repository.tests;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;

@Repository
public interface ExamRepository extends ReactiveCrudRepository<Exam, Integer> {

    @Query("select * from exams where groupID = :groupID order by date limit :limit offset :offset")
    Flux<Exam> sortByDate(@Param("limit") int limit, @Param("offset") int offset, @Param("groupID") int group);

    @Query("select * from exams where groupID in (:groups) order by date limit :limit offset :offset")
    Flux<Exam> getExamsFromGroups(@Param("limit") int limit, @Param("offset") int offset, @Param("groups") Collection<Integer> groups);

}
