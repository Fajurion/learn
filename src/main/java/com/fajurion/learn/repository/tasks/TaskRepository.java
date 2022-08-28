package com.fajurion.learn.repository.tasks;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TaskRepository extends ReactiveCrudRepository<Task, Integer> {

    @Query("select * from tasks where topic = :topic order by likes DESC limit :limit offset :offset")
    Flux<Task> sortTasksByLikes(@Param("topic") int topic,@Param("limit") int limit, @Param("offset") int offset);

    @Query("select * from tasks where topic = :topic and difficulty = :difficulty order by likes DESC limit :limit offset :offset")
    Flux<Task> sortTasksByLikesDifficulty(@Param("topic") int topic, @Param("limit") int limit, @Param("offset") int offset, @Param("difficulty") int difficulty);

    @Query("select * from tasks where topic = :topic and difficulty = :difficulty order by date DESC limit :limit offset :offset")
    Flux<Task> sortTasksByDateDifficulty(@Param("topic") int topic, @Param("limit") int limit, @Param("offset") int offset, @Param("difficulty") int difficulty);

    @Query("select * from tasks where topic = :topic order by date DESC limit :limit offset :offset")
    Flux<Task> sortTasksByDate(@Param("topic") int topic, @Param("limit") int limit, @Param("offset") int offset);

    @Query("select * from tasks where topic = :topic and (title like :query or task like :query) order by date DESC limit :limit offset :offset")
    Flux<Task> searchTasksByDate(@Param("topic") int topic, @Param("limit") int limit, @Param("offset") int offset, @Param("query") String query);

    @Query("select * from tasks where topic = :topic and (title like :query or task like :query) and difficulty = :difficulty order by date DESC limit :limit offset :offset")
    Flux<Task> searchTasksByDateDifficulty(@Param("topic") int topic, @Param("limit") int limit, @Param("offset") int offset, @Param("query") String query, @Param("difficulty") int difficulty);

    @Query("select * from tasks where topic = :topic and (title like :query or task like :query) and difficulty = :difficulty order by likes DESC limit :limit offset :offset")
    Flux<Task> searchTasksByLikesDifficulty(@Param("topic") int topic, @Param("limit") int limit, @Param("offset") int offset, @Param("query") String query, @Param("difficulty") int difficulty);

    @Query("select * from tasks where topic = :topic and (title like :query or task like :query) order by likes DESC limit :limit offset :offset")
    Flux<Task> searchTasksByLikes(@Param("topic") int topic, @Param("limit") int limit, @Param("offset") int offset, @Param("query") String query);

}
