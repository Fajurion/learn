package com.fajurion.learn.repository.tasks;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TaskRepository extends ReactiveCrudRepository<Task, Integer> {

    @Query("select * from tasks where topic = :topic order by likes DESC limit :limit offset :offset")
    Flux<Task> sortTasksByLikes(@Param("topic") int topic,@Param("limit") int limit,@Param("offset") int offset);

}
