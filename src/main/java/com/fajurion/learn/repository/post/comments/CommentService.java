package com.fajurion.learn.repository.post.comments;

import com.fajurion.learn.repository.account.Account;
import com.fajurion.learn.repository.account.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommentService {

    // Repository for getting comment data
    private final CommentRepository commentRepository;

    // Repository for getting account data
    private final AccountRepository accountRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository, AccountRepository accountRepository) {
        this.commentRepository = commentRepository;
        this.accountRepository = accountRepository;
    }

    public Mono<List<CommentResponse>> sortComments(int post, int limit, int offset) {

        // Get comments
        return commentRepository.sortCommentsByPost(post, limit, offset).flatMap(comment -> {

            // Zip with creator account
            return Mono.zip(Mono.just(comment), accountRepository.findById(comment.getCreator()).onErrorReturn(new Account("", "", "", "", "", -1)));
        }).collectList().map(list -> {
            ArrayList<CommentResponse> commentResponses = new ArrayList<>();

            // Turn tuple list into comment response list
            for(Tuple2<Comment, Account> tuple : list) {
                commentResponses.add(new CommentResponse(tuple.getT1(), tuple.getT2().getUsername()));
            }

            return commentResponses;
        });
    }

}
