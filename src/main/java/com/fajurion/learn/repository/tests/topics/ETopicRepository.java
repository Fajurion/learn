package com.fajurion.learn.repository.tests.topics;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ETopicRepository extends ReactiveCrudRepository<ETopic, Integer> {

    @Query("select * from exam_topics where test = :exam order by id limit :limit offset :offset")
    Flux<ETopic> sortTopics(@Param("test") int exam, @Param("limit") int limit, @Param("offset") int offset);

    @Query("select * from exam_topics where test = :test and topic = :topic")
    Flux<ETopic> getETopicsByTopicAndTest(@Param("test") int test, @Param("topic") int topic);

}
