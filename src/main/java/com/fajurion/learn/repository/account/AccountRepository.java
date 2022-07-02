package com.fajurion.learn.repository.account;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AccountRepository extends ReactiveCrudRepository<Account, Integer> {

    Flux<Account> getAccountsByUsername(@Param("username") String username);

    Flux<Account> getAccountsByEmail(@Param("email") String email);

    Mono<Account> getAccountByUsernameIgnoreCase(@Param("username") String username);

    Mono<Account> getAccountByEmailIgnoreCase(@Param("email") String email);

}
