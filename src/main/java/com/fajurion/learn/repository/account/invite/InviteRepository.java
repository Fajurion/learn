package com.fajurion.learn.repository.account.invite;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InviteRepository extends ReactiveCrudRepository<Invite, String> {
}
