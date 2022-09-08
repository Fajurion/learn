package com.fajurion.learn.repository.topic;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface TopicRepository extends ReactiveCrudRepository<Topic, Integer> {

    @Query("select * from topics where parent = :parent order by id")
    Flux<Topic> getTopicsByParent(@Param("parent") int parent);

    Mono<Long> countTopicsByParent(@Param("parent") int parent);

    @Query("select * from topics where name like :query order by id limit :limit offset :offset")
    Flux<Topic> searchTopicsByName(@Param("query") String query, @Param("limit") int limit, @Param("offset") int offset);

    @Query("select * from topics where name like :query and parent = :parent order by id limit :limit offset :offset")

    Flux<Topic> searchTopicsByNameWithParent(@Param("query") String query, @Param("limit") int limit, @Param("offset") int offset, @Param("parent") int parent);

}
