package com.fajurion.learn.repository.account.tfa;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TwoFactorRepository extends ReactiveCrudRepository<TwoFactor, Integer> {

    Flux<TwoFactor> getTwoFactorByAccount(@Param("account") int account);

}
