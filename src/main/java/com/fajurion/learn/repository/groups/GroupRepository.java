package com.fajurion.learn.repository.groups;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface GroupRepository extends ReactiveCrudRepository<Group, Integer> {

    Mono<Group> getGroupByNameIgnoreCase(@Param("name") String name);

    @Query("select * from groups limit :limit offset :offset")
    Flux<Group> getGroupsBy(@Param("limit") int limit, @Param("offset") int offset);

}
