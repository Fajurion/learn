package com.fajurion.learn.repository.post;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Repository
public interface PostRepository extends ReactiveCrudRepository<Post, Integer> {

    // Deletion method
    Mono<Void> deleteAllByTopic(@Param("topic") int topic);

    // Sort with likes descending with limit and offset
    @Query("select * from posts where topic = :topic order by likes DESC LIMIT :limit OFFSET :offset")
    Flux<Post> sortPostsByLikes(@Param("topic") int topic, @Param("limit") int limit, @Param("offset") int offset);

    // Sort by date descending with limit and offset
    @Query("select * from posts where topic = :topic order by date DESC LIMIT :limit OFFSET :offset")
    Flux<Post> sortPostsByDate(@Param("topic") int topic, @Param("limit") int limit, @Param("offset") int offset);

    // Sort by date descending with limit and offset and search for title
    @Query("select * from posts where topic = :topic and title like :query order by date DESC LIMIT :limit OFFSET :offset")
    Flux<Post> searchPostsByDate(@Param("query") String query, @Param("topic") int topic, @Param("limit") int limit, @Param("offset") int offset);

    // Sort by date descending with limit and offset and search for title
    @Query("select * from posts where topic = :topic and title like :query order by likes DESC LIMIT :limit OFFSET :offset")
    Flux<Post> searchPostsByLikes(@Param("query") String query, @Param("topic") int topic, @Param("limit") int limit, @Param("offset") int offset);

}
